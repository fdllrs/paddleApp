package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.model.Club
import com.paddle.app.model.Match
import com.paddle.app.model.MatchPlayer
import com.paddle.app.model.MatchStatus
import com.paddle.app.model.User
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.MatchPlayerRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.dao.DataIntegrityViolationException
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class MatchServiceTest {

    @MockK
    private lateinit var matchRepository: MatchRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var clubRepository: ClubRepository

    @MockK
    private lateinit var matchPlayerRepository: MatchPlayerRepository

    @InjectMockKs
    private lateinit var matchService: MatchService

    private val fixedDateTime: OffsetDateTime = OffsetDateTime.parse("2026-01-10T18:00:00Z")
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    private fun testMatchCreateRequestDTO(
        hostId: UUID = UUID.randomUUID(),
        clubId: UUID = UUID.randomUUID(),
        matchDate: OffsetDateTime = fixedDateTime
    ): MatchCreateRequestDTO =
        MatchCreateRequestDTO(
            hostId = hostId,
            clubId = clubId,
            matchDate = matchDate,
            durationMinutes = 90,
            pricePerPerson = 15f,
            targetDivision = 5
        )

    private fun testUser(
        id: UUID = UUID.randomUUID(),
        name: String = "Test User",
        division: Int = 5
    ): User =
        User(
            id = id,
            displayName = name,
            division = division
        )

    private fun testClub(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Club"
    ): Club =
        Club(
            id = id,
            name = name,
            address = "123 St",
            location = geometryFactory.createPoint(Coordinate(-58.0, -34.0))
        )

    private fun testMatch(
        id: UUID = UUID.randomUUID(),
        host: User = testUser(),
        club: Club = testClub(),
        date: OffsetDateTime = fixedDateTime,
        status: MatchStatus = MatchStatus.OPEN
    ): Match =
        Match(
            id = id,
            host = host,
            club = club,
            matchDate = date,
            durationMinutes = 90,
            pricePerPerson = 15f,
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

    private fun givenClubExists(club: Club) {
        every { clubRepository.findById(requireNotNull(club.id)) } returns Optional.of(club)
    }

    private fun givenMatchExists(match: Match) {
        every { matchRepository.findById(requireNotNull(match.id)) } returns Optional.of(match)
    }

    private fun givenUserMissing(userId: UUID) {
        every { userRepository.findById(userId) } returns Optional.empty()
    }

    private fun givenClubMissing(clubId: UUID) {
        every { clubRepository.findById(clubId) } returns Optional.empty()
    }

    private fun givenMatchMissing(matchId: UUID) {
        every { matchRepository.findById(matchId) } returns Optional.empty()
    }

    @Nested
    inner class CreateMatchTest {

        @Test
        fun `createMatch should save match and automatically join the host`() {
            // Arrange
            val host = testUser(name = "John Doe")
            val club = testClub(name = "Test Club")
            val request = testMatchCreateRequestDTO(
                hostId = requireNotNull(host.id),
                clubId = requireNotNull(club.id),
                matchDate = fixedDateTime
            )
            val match = testMatch(host = host, club = club, date = request.matchDate)

            givenUserExists(host)
            givenClubExists(club)
            givenMatchExists(match)


            every { matchRepository.save(any()) } returns match
            every { matchPlayerRepository.findByMatchId(match.id!!) } returns listOf(testMatchPlayer(player = host, match = match))
            every { matchPlayerRepository.save(any()) } returns mockk()

            // Act
            val response = matchService.createMatch(request)

            // Assert
            assertEquals(match.id, response.id)
            assertEquals(club.name, response.clubName)
            assertEquals(host.displayName, response.hostName)
            verify(exactly = 1) { matchRepository.save(any()) }
            verify(exactly = 1) { matchPlayerRepository.save(any()) }
        }

        @Test
        fun `createMatch should throw exception when club is not found`() {
            // Arrange
            val host = testUser()
            val request = testMatchCreateRequestDTO(
                hostId = requireNotNull(host.id),
                clubId = UUID.randomUUID()
            )

            givenUserExists(host)
            givenClubMissing(request.clubId)

            // Act
            val exception = assertThrows<IllegalArgumentException> {
                matchService.createMatch(request)
            }

            // Assert
            assertEquals(MatchService.CLUB_NOT_FOUND_MESSAGE, exception.message)
            verify(exactly = 0) { matchRepository.save(any()) }
        }
    }

    @Nested
    inner class JoinMatchTest {

        @Test
        fun `joinMatch correctly joins a user to a match`() {
            // Arrange
            val player = testUser(name = "Player")
            val host = testUser(name = "Host")
            val club = testClub()
            val match = testMatch(host = host, club = club)

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
        fun `joinMatch should throw exception when match is full`() {
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

            every {
                matchRepository.findNearbyMatches(any(), any(), any(), any())
            } returns listOf(testMatch())

            // Act
            matchService.getNearbyOpenMatches(latitude, longitude, radiusMeters, targetDivision)

            // Assert
            verify(exactly = 1) {
                matchRepository.findNearbyMatches(
                    eq(MatchStatus.OPEN),
                    withArg { point ->
                        assertEquals(latitude, point.y)
                        assertEquals(longitude, point.x)
                    },
                    eq(radiusMeters),
                    eq(targetDivision)
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
