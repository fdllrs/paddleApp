package com.paddle.app.service

import com.paddle.app.model.*
import com.paddle.app.repository.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class MatchServiceConcurrencyTest @Autowired constructor(
    private val matchService: MatchService,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val courtRepository: CourtRepository,
    private val matchRepository: MatchRepository,
    private val matchPlayerRepository: MatchPlayerRepository
) {

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    fun cleanUp() {
        matchPlayerRepository.deleteAll()
        matchRepository.deleteAll()
        courtRepository.deleteAll()
        clubRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `concurrent joins should never exceed maximum players per match`() {
        // Arrange
        val host = userRepository.save(testUser(displayName = "Host"))

        val challengers = (1..5).map { index ->
            userRepository.save(testUser(displayName = "Player $index"))
        }

        val club = clubRepository.save(testClub())
        val court = courtRepository.save(testCourt(club = club))

        val match = matchRepository.save(
            Match(
                host = host,
                court = court,
                status = MatchStatus.OPEN,
                startDate = OffsetDateTime.parse("2026-01-10T18:00:00Z"),
                endDate = OffsetDateTime.parse("2026-01-10T19:30:00Z"),
                targetDivision = 5,
                durationMinutes = 90,
                pricePerPerson = BigDecimal(15)
            )
        )

        matchPlayerRepository.save(MatchPlayer(match = match, player = host))

        val matchId = requireNotNull(match.id)
        val threadCount = challengers.size
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)

        val successfulJoins = AtomicInteger(0)
        val failedJoins = AtomicInteger(0)

        val tasks = challengers.map { player ->
            Callable {
                readyLatch.countDown()
                startLatch.await()

                try {
                    matchService.joinMatch(matchId, requireNotNull(player.id))
                    successfulJoins.incrementAndGet()
                } catch (_: Exception) {
                    failedJoins.incrementAndGet()
                }
            }
        }

        // Act
        val futures = tasks.map { task -> executor.submit(task) }

        assertTrue(
            readyLatch.await(5, TimeUnit.SECONDS),
            "All join tasks should be ready before starting the concurrency test"
        )

        startLatch.countDown()

        futures.forEach { future ->
            future.get(10, TimeUnit.SECONDS)
        }

        executor.shutdown()
        assertTrue(
            executor.awaitTermination(5, TimeUnit.SECONDS),
            "Executor should terminate cleanly"
        )

        // Assert
        val finalPlayerCount = matchPlayerRepository.countByMatchId(matchId)
        val finalMatch = matchRepository.findById(matchId).orElseThrow()

        assertEquals(
            MatchService.MAX_PLAYERS_PER_MATCH,
            finalPlayerCount,
            "Concurrent joins must not create more than ${MatchService.MAX_PLAYERS_PER_MATCH} players"
        )

        assertEquals(
            MatchStatus.FULL,
            finalMatch.status,
            "Match should be marked FULL once it reaches capacity"
        )

        assertEquals(
            MatchService.MAX_PLAYERS_PER_MATCH - 1,
            successfulJoins.get(),
            "Only the available non-host slots should be successfully joined"
        )

        assertEquals(
            challengers.size - successfulJoins.get(),
            failedJoins.get(),
            "Extra concurrent join attempts should fail"
        )
    }

    private fun testUser(
        id: UUID? = null,
        displayName: String = "Test User"
    ): User =
        User(
            id = id,
            firebaseUid = "test firebase id - ${displayName.lowercase()}",
            displayName = displayName,
            division = 5
        )

    private fun testClub(): Club =
        Club(
            name = "Concurrency Test Club",
            address = "Test address",
            coordinates = geometryFactory.createPoint(Coordinate(-58.0, -34.0)),
            openTime = OffsetTime.parse("10:00:00Z"),
            closeTime = OffsetTime.parse("22:00:00Z"),
            neighborhood = "Test neighborhood"
        )

    private fun testCourt(club: Club): Court =
        Court(
            club = club,
            name = "Concurrency Test Court",
            pricePerTurn = BigDecimal("60.00"),
            covered = false,
            wallType = WallType.GLASS,
            floorType = FloorType.SYNTHETIC_GRASS
        )
}