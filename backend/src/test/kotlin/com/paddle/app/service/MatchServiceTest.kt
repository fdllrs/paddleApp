package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.model.*
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchPlayerRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.*
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class MatchServiceTest {

    @MockK
    private lateinit var matchRepository: MatchRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var courtRepository: CourtRepository

    @MockK
    private lateinit var matchPlayerRepository: MatchPlayerRepository

    @InjectMockKs
    private lateinit var matchService: MatchService

    private val fixedDateTime: OffsetDateTime = OffsetDateTime.parse("2026-01-10T18:00:00Z")
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun testMatchCreateRequestDTO(
        hostId: UUID = UUID.randomUUID(),
        courtId: UUID = UUID.randomUUID(),
        matchDate: OffsetDateTime = fixedDateTime
    ): MatchCreateRequestDTO =
        MatchCreateRequestDTO(
            hostId = hostId,
            courtId = courtId,
            startDate = matchDate,
            durationMinutes = 90,
            pricePerPerson = 15.toBigDecimal(),
            targetDivision = 5
        )

    private fun testUser(
        id: UUID = UUID.randomUUID(),
        division: Int = 5
    ): User =
        User(
            id = id,
            displayName = "Test User",
            division = division
        )

    private fun testClub(
        id: UUID = UUID.randomUUID()
    ): Club =
        Club(
            id = id,
            name = "Test Club",
            address = "123 St",
            location = geometryFactory.createPoint(Coordinate(-58.0, -34.0)),
            openTime = OffsetTime.parse("10:00:00Z"),
            closeTime = OffsetTime.parse("22:00:00Z"),
            neighborhood = "VILLA ADELINA"
        )
    private fun testCourt(
        id: UUID = UUID.randomUUID(),
        club: Club = testClub()
    ): Court = Court(
        id = id,
        club = club,
        name = "test Court",
        pricePerTurn = 15.toBigDecimal(),
        covered = false,
        wallType = WallType.CEMENT,
        floorType = FloorType.SYNTHETIC_GRASS
    )


    private fun testMatch(
        id: UUID = UUID.randomUUID(),
        host: User = testUser(),
        court: Court = testCourt(),
        startDate: OffsetDateTime = fixedDateTime,
        durationMinutes: Int = 90,
        status: MatchStatus = MatchStatus.OPEN
    ): Match =
        Match(
            id = id,
            host = host,
            court = court,
            startDate = startDate,
            endDate = startDate.plusMinutes(durationMinutes.toLong()),
            durationMinutes = durationMinutes,
            pricePerPerson = 15.toBigDecimal(),
            targetDivision = 5,
            status = status
        )

    private fun testMatchPlayer(
        player: User = testUser(),
        match: Match = testMatch()
    ): MatchPlayer =
        MatchPlayer(
            player = player,
            match = match
        )

    private fun givenUserExists(user: User) {
        every { userRepository.findById(requireNotNull(user.id)) } returns Optional.of(user)
    }

    private fun givenCourtExists(court: Court) {
        every { courtRepository.findCourtById(requireNotNull(court.id)) } returns court
    }

    private fun givenMatchExists(match: Match) {
        every { matchRepository.findById(requireNotNull(match.id)) } returns Optional.of(match)
    }

    private fun givenUserMissing(userId: UUID) {
        every { userRepository.findById(userId) } returns Optional.empty()
    }

    private fun givenCourtMissing(courtId: UUID) {
        every { courtRepository.findCourtById(courtId) } returns null
    }

    private fun givenMatchMissing(matchId: UUID) {
        every { matchRepository.findById(matchId) } returns Optional.empty()
    }

    private fun requestHasNoOverlappingMatches() {
        every { matchRepository.overlappingMatches(any(), any(), any()) } returns emptyList()
    }

    private fun verifyMatchSavedWithCorrectDates(request: MatchCreateRequestDTO) {
        verify(exactly = 1) {
            matchRepository.save(withArg { savedMatch ->
                savedMatch.startDate == request.startDate &&
                        savedMatch.endDate == request.startDate.plusMinutes(request.durationMinutes.toLong())

            })
        }
    }


    @Nested
    inner class CreateMatchTest {

        @Test
        fun `createMatch should save match and automatically join the host`() {
            // Arrange
            val host = testUser()
            val court = testCourt()
            val courtId = requireNotNull(court.id)
            val request = testMatchCreateRequestDTO(
                hostId = requireNotNull(host.id),
                courtId = requireNotNull(court.id),
                matchDate = fixedDateTime
            )
            val match = testMatch(host = host, court = court, startDate = request.startDate)
            val matchId = requireNotNull(match.id)

            givenUserExists(host)
            givenCourtExists(court)
            givenMatchExists(match)
            requestHasNoOverlappingMatches()


            every { matchRepository.save(any()) } returns match
            every { matchPlayerRepository.findByMatchId(match.id!!) } returns listOf(
                testMatchPlayer(
                    player = host,
                    match = match
                )
            )
            every { matchPlayerRepository.save(any()) } returns mockk()
            // Act
            val response = matchService.createMatch(request)

            // Assert
            verifyOrder {
                courtRepository.findCourtById(courtId)
                matchRepository.overlappingMatches(courtId, fixedDateTime, fixedDateTime.plusMinutes(90))
            }

            assertEquals(matchId, response.id)
            assertEquals(court.club.name, response.clubName)
            assertEquals(host.displayName, response.hostName)
            verifyMatchSavedWithCorrectDates(request)
            verify(exactly = 1) {
                matchPlayerRepository.save(withArg { savedMatchPlayer ->
                    assertEquals(savedMatchPlayer.player.id, host.id)
                    assertEquals(savedMatchPlayer.match.id, matchId)
                })
            }
        }

        @Test
        fun `createMatch should throw exception when court is not found`() {
            // Arrange
            val host = testUser()
            val request = testMatchCreateRequestDTO(
                hostId = requireNotNull(host.id),
                courtId = UUID.randomUUID()
            )

            givenUserExists(host)
            givenCourtMissing(request.courtId)
            requestHasNoOverlappingMatches()

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.createMatch(request)
            }

            // Assert
            assertEquals(MatchService.COURT_NOT_FOUND_MESSAGE, exception.message)
            verify(exactly = 0) { matchRepository.save(any()) }

        }

        @Test
        fun `createMatch should throw exception when user is not found`() {
            // Arrange
            val host = testUser()
            val court = testCourt()
            val courtId = requireNotNull(court.id)
            val request = testMatchCreateRequestDTO(
                hostId = requireNotNull(host.id),
                courtId = courtId
            )

            givenUserMissing(requireNotNull(host.id))
            givenCourtExists(court)
            requestHasNoOverlappingMatches()

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.createMatch(request)
            }

            // Assert
            assertEquals(MatchService.USER_NOT_FOUND_MESSAGE, exception.message)
            verify(exactly = 0) { matchRepository.save(any()) }

        }

        @Test
        fun `createMatch should not allow user to create matches with equal start times`() {

            val host = testUser()
            val court = testCourt()
            val courtId = requireNotNull(court.id)
            val bookedMatch = testMatch(startDate = fixedDateTime)
            val request = testMatchCreateRequestDTO(courtId = courtId, hostId = requireNotNull(host.id), matchDate = fixedDateTime)


            givenCourtExists(court)
            givenUserExists(host)
            every { matchRepository.overlappingMatches(courtId, fixedDateTime, fixedDateTime.plusMinutes(90)) } returns listOf(bookedMatch)

            val exception = assertThrows<IllegalArgumentException> {
                matchService.createMatch(request)
            }

            // Assert
            assertEquals(MatchService.COURT_ALREADY_BOOKED_MESSAGE, exception.message)
            verifyOrder {
                    courtRepository.findCourtById(courtId)
                    matchRepository.overlappingMatches(courtId, fixedDateTime, fixedDateTime.plusMinutes(90))
                    }
            verify(exactly = 0) { matchRepository.save(any()) }
        }

       }

    @Nested
    inner class JoinMatchTest {

        @Test
        fun `joinMatch correctly joins a user to a match`() {
            // Arrange
            val player = testUser()
            val host = testUser()
            val court = testCourt()
            val match = testMatch(host = host, court = court)

            givenUserExists(player)
            givenMatchExists(match)
            every { matchPlayerRepository.save(any()) } returns mockk()
            every { matchPlayerRepository.findByMatchId(match.id!!) } returns listOf(testMatchPlayer(player = host, match = match))

            // Act
            matchService.joinMatch(requireNotNull(match.id), requireNotNull(player.id))

            // Assert
            verify(exactly = 1) {
                matchPlayerRepository.save(
                    withArg { savedMatchPlayer ->
                        assertEquals(match.id, savedMatchPlayer.match.id)
                        assertEquals(player.id, savedMatchPlayer.player.id)
                    }
                )
            }
        }

        @Test
        fun `joinMatch should throw exception when user is missing`() {
            // Arrange
            val match = testMatch()
            val missingUserId = UUID.randomUUID()

            givenMatchExists(match)
            givenUserMissing(missingUserId)

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.joinMatch(requireNotNull(match.id), missingUserId)
            }

            // Assert
            assertEquals(MatchService.USER_NOT_FOUND_MESSAGE, exception.message)
            verify(exactly = 0) { matchPlayerRepository.save(any()) }
        }

        @Test
        fun `joinMatch should throw exception when match is missing`() {
            // Arrange
            val missingMatchId = UUID.randomUUID()
            val userId = UUID.randomUUID()

            givenMatchMissing(missingMatchId)

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.joinMatch(missingMatchId, userId)
            }

            // Assert
            assertEquals(MatchService.MATCH_NOT_FOUND_MESSAGE, exception.message)
            verify(exactly = 0) { matchPlayerRepository.save(any()) }
        }

        @Test
        fun `joinMatch should throw exception when match is not open`() {
            // Arrange
            val player = testUser()
            val closedMatch = testMatch(status = MatchStatus.FULL)

            givenMatchExists(closedMatch)
            givenUserExists(player)

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.joinMatch(requireNotNull(closedMatch.id), requireNotNull(player.id))
            }

            // Assert
            assertEquals(MatchService.MATCH_FULL_MESSAGE, exception.message)
            verify(exactly = 0) { matchPlayerRepository.save(any()) }
        }

        @Test
        fun `joinMatch should not join the same user twice`() {
            // Arrange
            val player = testUser()
            val host = testUser()
            val match = testMatch(host = host)

            givenMatchExists(match)
            givenUserExists(player)
            every {
                matchPlayerRepository.save(any())
            } throws DataIntegrityViolationException(MatchService.USER_ALREADY_IN_MATCH_MESSAGE)

            // Act
            val exception = assertThrows<DataIntegrityViolationException> {
                matchService.joinMatch(requireNotNull(match.id), requireNotNull(player.id))
            }

            // Assert
            assertEquals(MatchService.USER_ALREADY_IN_MATCH_MESSAGE, exception.message)
        }
    }

    @Nested
    inner class GetNearbyMatchesTest {

        @Test
        fun `getNearbyMatches should translate latitude and longitude to point coordinates correctly`() {
            // Arrange
            val latitude = -34.0
            val longitude = -58.0
            val radiusMeters = 5000.0
            val targetDivision = 7
            val pagedResponse = PageImpl(listOf(testMatch()))


            every {
                matchRepository.findNearbyMatches(any(), any(), any(), any(), any())
            } returns pagedResponse

            // Act
            matchService.getNearbyOpenMatches(latitude, longitude, radiusMeters, targetDivision, Pageable.unpaged())

            // Assert
            verify(exactly = 1) {
                matchRepository.findNearbyMatches(
                    eq(MatchStatus.OPEN),
                    withArg { point ->
                        assertEquals(latitude, point.y)
                        assertEquals(longitude, point.x)
                    },
                    eq(radiusMeters),
                    eq(targetDivision),
                    any()
                )
            }
        }
    }

    @Nested
    inner class LeaveMatchTest {

        @Test
        fun `match host cannot leave the match`() {
            // Arrange
            val host = testUser()
            val match = testMatch(host = host)

            givenMatchExists(match)
            givenUserExists(host)

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.leaveMatch(requireNotNull(match.id), requireNotNull(host.id))
            }

            // Assert
            assertEquals(MatchService.HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE, exception.message)
            verify(exactly = 0) { matchPlayerRepository.delete(any()) }
        }

        @Test
        fun `player cannot leave a match they are not a part of`() {
            // Arrange
            val player = testUser()
            val host = testUser()
            val match = testMatch(host = host)

            givenMatchExists(match)
            givenUserExists(player)
            every {
                matchPlayerRepository.findByMatchIdAndPlayerId(requireNotNull(match.id), requireNotNull(player.id))
            } returns null

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.leaveMatch(requireNotNull(match.id), requireNotNull(player.id))
            }

            // Assert
            assertEquals(MatchService.USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE, exception.message)
            verify(exactly = 0) { matchPlayerRepository.delete(any()) }
        }

        @Test
        fun `leaveMatch should delete the matchPlayer record for the player`() {
            // Arrange
            val match = testMatch()
            val player = testUser()
            val existingMembership = testMatchPlayer(player = player, match = match)

            givenMatchExists(match)
            givenUserExists(player)
            every {
                matchPlayerRepository.findByMatchIdAndPlayerId(requireNotNull(match.id), requireNotNull(player.id))
            } returns existingMembership
            every { matchPlayerRepository.delete(any()) } just Runs

            // Act
            matchService.leaveMatch(requireNotNull(match.id), requireNotNull(player.id))

            // Assert
            verify(exactly = 1) { matchPlayerRepository.delete(existingMembership) }
        }
    }

    @Nested
    inner class CancelMatchTest {

        @Test
        fun `cancelMatch should set the status to cancelled`() {
            // Arrange
            val host = testUser()
            val match = testMatch(host = host)

            givenMatchExists(match)
            givenUserExists(host)
            every { matchRepository.save(any()) } returns match

            // Act
            matchService.cancelMatch(requireNotNull(match.id), requireNotNull(host.id))

            // Assert
            assertTrue { match.isCancelled() }
            verify(exactly = 1) { matchRepository.save(match) }
        }

        @Test
        fun `cancelMatch should not allow user other than host to cancel`(){
            val user = testUser()
            val host = testUser()
            val match = testMatch(host = host)

            val matchId = requireNotNull(match.id)
            val userId = requireNotNull(user.id)

            givenMatchExists(match)
            givenUserExists(host)
            givenUserExists(user)

            val exception = assertThrows<SecurityException> { matchService.cancelMatch(matchId, userId) }


            assertEquals(MatchService.ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE, exception.message)
            verify(exactly = 0) { matchRepository.save(any()) }
        }


    }

}
