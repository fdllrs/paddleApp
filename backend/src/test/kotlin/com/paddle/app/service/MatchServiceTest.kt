//package com.paddle.app.service
//
//import com.paddle.app.dto.MatchCreateRequestDTO
//import com.paddle.app.model.Club
//import com.paddle.app.model.Match
//import com.paddle.app.model.User
//import com.paddle.app.repository.ClubRepository
//import com.paddle.app.repository.MatchPlayerRepository
//import com.paddle.app.repository.MatchRepository
//import com.paddle.app.repository.UserRepository
//import io.mockk.every
//import io.mockk.impl.annotations.InjectMockKs
//import io.mockk.impl.annotations.MockK
//import io.mockk.junit5.MockKExtension
//import io.mockk.mockk
//import io.mockk.verify
//import jakarta.persistence.Id
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import org.junit.jupiter.api.extension.ExtendWith
//import org.locationtech.jts.geom.Coordinate
//import org.locationtech.jts.geom.GeometryFactory
//import org.locationtech.jts.geom.PrecisionModel
//import org.springframework.dao.DataIntegrityViolationException
//import java.time.Instant
//import java.time.OffsetDateTime
//import java.time.ZoneId
//import java.util.UUID
//import java.util.Optional
//
//@ExtendWith(MockKExtension::class)
//class MatchServiceTest {
//
//    // 1. Mock the dependencies (The Database layer)
//    @MockK
//    private lateinit var matchRepository: MatchRepository
//    @MockK
//    private lateinit var userRepository: UserRepository
//    @MockK
//    private lateinit var clubRepository: ClubRepository
//    @MockK
//    private lateinit var matchPlayerRepository: MatchPlayerRepository
//    @MockK
//    private lateinit var clock: java.time.Clock
//
//    @InjectMockKs
//    private lateinit var matchService: MatchService
//
//    private fun mockCreateRequestDTO(
//        hostId: UUID= UUID.randomUUID(),
//        clubId: UUID= UUID.randomUUID()): MatchCreateRequestDTO {
//        val request = MatchCreateRequestDTO(
//            hostId = hostId,
//            clubId = clubId,
//            matchDate = OffsetDateTime.now(),
//            durationMinutes = 90,
//            pricePerPerson = 15f,
//            targetDivision = 5
//        )
//        return request
//    }
//
//    private fun mockUser(
//        userId: UUID=UUID.randomUUID(),
//        name: String = "Test User"): User {
//        val mockUser = User(
//            id = userId,
//            displayName = name,
//            division = 5
//        )
//        return mockUser
//    }
//
//    private fun mockClub(
//        clubId: UUID=UUID.randomUUID(),
//        name: String = "Test Club",
//    ): Club {
//        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
//
//        val club = Club(
//            id = clubId,
//            name = name,
//            address = "123 St",
//            location = geometryFactory.createPoint(Coordinate(-58.0, -34.0))
//        )
//        return club
//    }
//
//    private fun mockMatch(
//        host: User = mockUser(),
//        club: Club = mockClub(),
//        date: OffsetDateTime = OffsetDateTime.now(),
//        status: String = MatchService.STATUS_OPEN
//    ): Match = Match(
//        id = UUID.randomUUID(),
//        host = host,
//        club = club,
//        matchDate = date,
//        durationMinutes = 90,
//        pricePerPerson = 15f,
//        targetDivision = 5,
//        status = status
//    )
//
//
//    @Nested
//    inner class CreateMatchTest{
//
//        @Test
//        fun `createMatch should save match and automatically join the host`() {
//
//            val userName = "John Doe"
//            val clubName = "Test Club"
//            val mockUser = mockUser(name = userName)
//            val mockClub = mockClub(name = clubName)
//            val userId = requireNotNull(mockUser.id)
//            val clubId = requireNotNull(mockClub.id)
//            val request = mockCreateRequestDTO(userId, clubId)
//
//            val mockMatch = mockMatch(mockUser, mockClub, request.matchDate)
//            val matchId = requireNotNull(mockMatch.id)
//
//            // Tell the mocks how to behave when the Service calls them
//            every { userRepository.findById(userId) } returns Optional.of(mockUser)
//            every { clubRepository.findById(clubId) } returns Optional.of(mockClub)
//            every { matchRepository.findById(matchId) } returns Optional.of(mockMatch)
//            every { matchRepository.save(any()) } returns mockMatch
//            every { matchPlayerRepository.save(any()) } returns mockk()
//
//            // WHEN (Act)
//            val response = matchService.createMatch(request)
//
//            // THEN (Assert)
//            assertEquals(mockMatch.id, response.id)
//            assertEquals(clubName, response.clubName)
//            assertEquals(userName, response.hostName)
//
//            // Verify that the repository methods were actually called exactly once
//            verify(exactly = 1) { matchRepository.save(any()) }
//            verify(exactly = 1) { matchPlayerRepository.save(any()) }
//        }
//
//        @Test
//        fun `createMatch should throw exception when club is not found`() {
//            // GIVEN
//            val request = mockCreateRequestDTO()
//            val mockUser = mockUser()
//
//            every { userRepository.findById(request.hostId) } returns Optional.of(mockUser)
//            every { clubRepository.findById(request.clubId) } returns Optional.empty() // Simulate database missing the club
//
//            // WHEN & THEN
//            val exception = assertThrows<IllegalArgumentException> {
//                matchService.createMatch(request)
//            }
//
//            assertEquals(MatchService.CLUB_NOT_FOUND_MESSAGE, exception.message)
//
//            // Ensure the database NEVER tried to save a broken match
//            verify(exactly = 0) { matchRepository.save(any()) }
//        }
//
//    }
//
//    @Nested
//    inner class JoinMatchTest{
//
//        @Test
//        fun `joinMatch correctly joins a user to a match`(){
//            val user = mockUser()
//            val host = mockUser()
//            val club = mockClub()
//            val match = mockMatch(host, club, OffsetDateTime.now())
//            val userId = requireNotNull(user.id)
//            val matchId = requireNotNull(match.id)
//
//            every { userRepository.findById(userId) } returns Optional.of(user)
//            every { matchRepository.findById(matchId) } returns Optional.of(match)
//            every { matchPlayerRepository.save(any()) } returns mockk()
//
//            matchService.joinMatch(matchId, userId)
//
//            verify(exactly = 1) { matchPlayerRepository.save(
//                withArg { capturedEntity ->
//                    // This lambda executes when the mock intercepts the save() call.
//                    // capturedEntity is the exact object your service instantiated.
//                    assertEquals(match.id, capturedEntity.match.id)
//                    assertEquals(user.id, capturedEntity.player.id)
//                }
//            ) }
//        }
//
//        @Test
//        fun `joinMatch should throw exception when user is missing`(){
//
//            val match = mockMatch()
//            val matchId = requireNotNull(match.id)
//
//            every { matchRepository.findById(matchId) } returns Optional.of(match)
//            every { userRepository.findById(any()) } returns Optional.empty()
//
//
//            val exception = assertThrows<IllegalArgumentException> {
//                matchService.joinMatch(matchId, UUID.randomUUID())
//            }
//
//            assertEquals(MatchService.USER_NOT_FOUND_MESSAGE, exception.message)
//
//            verify(exactly = 0) { matchPlayerRepository.save(any()) }
//
//        }
//
//        @Test
//        fun `joinMatch should throw exception when match is missing`(){
//            every { matchRepository.findById(any()) } returns Optional.empty()
//
//            val exception = assertThrows<IllegalArgumentException> {
//                matchService.joinMatch(UUID.randomUUID(), UUID.randomUUID())
//            }
//
//            assertEquals(MatchService.MATCH_NOT_FOUND_MESSAGE, exception.message)
//
//            verify(exactly = 0) { matchPlayerRepository.save(any()) }
//
//        }
//
//        @Test
//        fun `joinMatch should throw exception when match is not open`(){
//            val match = mockMatch(status = MatchService.STATUS_CLOSED)
//
//            val matchId = requireNotNull(match.id)
//
//            every { matchRepository.findById(matchId) } returns Optional.of(match)
//            every { userRepository.findById(any()) } returns Optional.of(mockUser())
//
//            val exception = assertThrows<IllegalArgumentException> {
//                matchService.joinMatch(matchId, UUID.randomUUID())
//            }
//
//            assertEquals(MatchService.MATCH_NOT_OPEN_MESSAGE, exception.message)
//
//            verify(exactly = 0) { matchPlayerRepository.save(any()) }
//
//        }
//
//        @Test
//        fun `joinMatch should not join the same user twice`(){
//
//            val user = mockUser()
//            val host = mockUser()
//            val match = mockMatch(host=host)
//
//            val userId = requireNotNull(user.id)
//            val hostId = requireNotNull(host.id)
//            val matchId = requireNotNull(match.id)
//
//            every { matchRepository.findById(matchId) } returns Optional.of(match)
//            every { userRepository.findById(hostId) } returns Optional.of(host)
//            every { userRepository.findById(userId) } returns Optional.of(user)
//            every { matchPlayerRepository.save(any()) } throws DataIntegrityViolationException("User already joined this match.")
//
//            val exception = assertThrows<DataIntegrityViolationException> {
//                matchService.joinMatch(matchId, userId)
//            }
//
//
//
//            assertEquals("User already joined this match.", exception.message)
//
//
//        }
//
//    }
//
//    @Nested
//    inner class GetNearbyMatchesTest{
//
//        @Test
//        fun `getNearbyMatches should translates Longitude and Latitude to X Y correctly`(){
//            val testLatitude = -34.0
//            val testLongitude = -58.0
//            val testRadiusMeters = 5000.0
//
//            val mockMatch = mockMatch()
//
//            every { matchRepository.findNearbyMatches(any(), any(), any()) } returns listOf(mockMatch)
//
//            matchService.getNearbyOpenMatches(testLatitude, testLongitude, testRadiusMeters)
//
//            verify(exactly = 1) { matchRepository.findNearbyMatches(
//                eq(MatchService.STATUS_OPEN),
//                withArg { capturedPoint ->
//                    assertEquals(testLatitude, capturedPoint.y)
//                    assertEquals(testLongitude, capturedPoint.x)
//                },
//                eq(testRadiusMeters)
//            ) }
//
//
//        }
//    }
//
//    @Nested
//    inner class leaveMatchTest{
//        @BeforeEach
//        fun setup(){
//            val fixedTime = Instant.parse("2026-03-08T12:00:00Z")
//            val zoneId = ZoneId.of("UTC")
//
//            every { clock.instant() } returns fixedTime
//            every { clock.zone } returns zoneId
//
//        }
//
//        @Test
//        fun `cancelling a match less than 2 hours before the match should apply a penalization`(){
//            val user = mockUser()
//            val host = mockUser()
//            val club = mockClub()
//            val match = mockMatch(host, club, OffsetDateTime.now().minusHours(1))
//            val userId = requireNotNull(user.id)
//            val matchId = requireNotNull(match.id)
//
//
//
//
//
//
//
//        }
//
//
//
//
//
//
//    }
//
//}