package com.paddle.app.service

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.model.Club
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import com.paddle.app.model.User
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.time.*
import java.util.*
import kotlin.test.assertEquals


@ExtendWith(MockKExtension::class)
class MatchmakingServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var matchmakingTicketRepository: MatchmakingTicketRepository

    @MockK
    private lateinit var clubRepository: ClubRepository

    @MockK
    private lateinit var courtRepository: CourtRepository

    @MockK
    private lateinit var matchService: MatchService

    @SpyK
    private var geometryFactory = GeometryFactory()

    @InjectMockKs
    private lateinit var matchmakingService: MatchmakingService

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val fixedDateTime: OffsetDateTime = clock.instant().atOffset(ZoneOffset.UTC)


    private fun testQueueRequestDTO(
        startTime: OffsetDateTime = fixedDateTime,
        endTime: OffsetDateTime = fixedDateTime.plusHours(3),
        preferredDate: OffsetDateTime = fixedDateTime.plusHours(1),
    ): QueueRequestDTO = QueueRequestDTO(
        latitude = 0.0,
        longitude = 0.0,
        radiusMeters = 1000.0,
        startTime = startTime,
        endTime = endTime,
        preferredDate = preferredDate,
        preferredDurationMinutes = 60,
        preferredClubId = UUID.randomUUID(),
        preferredCourtId = UUID.randomUUID()
    )

    private fun testTicket(
        myId: UUID = UUID.randomUUID(),
        mockUser: User,
        searchLocation: Point,
        request: QueueRequestDTO
    ): MatchmakingTicket = MatchmakingTicket(
        id = UUID.randomUUID(),
        userId = myId,
        targetDivision = mockUser.division,
        searchLocation = searchLocation,
        maxRadiusMeters = request.radiusMeters,
        startTime = request.startTime,
        endTime = request.endTime,
        status = TicketStatus.SEARCHING,
        preferredClubId = request.preferredClubId,
        preferredCourtId = request.preferredCourtId,
        preferredMatchDate = request.preferredDate,
        preferredDurationMinutes = request.preferredDurationMinutes
    )

    @Test
    fun `should reject request if end time is before start time`() {
        val request = testQueueRequestDTO(
            startTime = fixedDateTime.plusHours(1),
            endTime = fixedDateTime,
        )

        val result = matchmakingService.isValidRequest(request)

        assertFalse(result)
    }

    @Test
    fun `can store a single player in queue`() {
        val myId = UUID.randomUUID()
        val mockUser = User(displayName = "Nazareno", id = myId, division = 3)

        val request = testQueueRequestDTO(
            startTime = fixedDateTime,
            endTime = fixedDateTime.plusHours(5),
            preferredDate = fixedDateTime.plusHours(4)
        )

        val searchLocation = geometryFactory.createPoint(
            Coordinate(request.longitude, request.latitude)
        )
        val newMatchmakingTicket = testTicket(myId, mockUser, searchLocation, request)


        every { userRepository.findUserById(myId) } returns mockUser
        every { matchmakingTicketRepository.findByUserIdAndStatus(any(), any())} returns null
        every { matchmakingTicketRepository.save(any()) } answers { newMatchmakingTicket }
        every { matchmakingTicketRepository.count() } returns 1L
        every { matchmakingTicketRepository.existsByUserId(myId) } returns true

        matchmakingService.joinQueue(request, myId)

        verify(exactly = 1) { matchmakingTicketRepository.save(any()) }
        assertFalse(matchmakingService.queueIsEmpty())
        assertTrue(matchmakingService.isPlayerInQueue(myId))
    }

    @Test
    fun `should return a list of clubs found in the intersection of two players`() {
        val p1Loc = geometryFactory.createPoint(Coordinate(40.4168, -3.7038))
        val p2Loc = geometryFactory.createPoint(Coordinate(40.4200, -3.7100))
        val radius = 2000.0

        val expectedClub = Club(
            id = UUID.randomUUID(),
            name = "Central Padel",
            location = geometryFactory.createPoint(Coordinate(40.4180, -3.7050)),
            address = "Central Padel address",
            neighborhood = "Central Padel neighborhood",
            openTime = OffsetTime.parse("10:00:00Z"),
            closeTime = OffsetTime.parse("22:00:00Z")
        )

        every {
            clubRepository.findClubsInIntersection(p1Loc, radius, p2Loc, radius)
        } returns listOf(expectedClub)

        val result = matchmakingService.getClubsForMatchmaking(p1Loc, p2Loc, radius, radius)

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(result[0], expectedClub)

        verify(exactly = 1) {
            clubRepository.findClubsInIntersection(p1Loc, radius, p2Loc, radius)
        }
    }
}