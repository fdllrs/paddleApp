package com.paddle.app.service

import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.repository.UserRepository
import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.User
import com.paddle.app.model.Club
import com.paddle.app.model.TicketStatus
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import com.paddle.app.repository.ClubRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.assertEquals


@ExtendWith(MockKExtension::class)
class MatchmakingServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var matchmakingTicketRepository: MatchmakingTicketRepository

    @MockK
    private lateinit var clubRepository: ClubRepository

    @SpyK
    private var geometryFactory = GeometryFactory()

    @InjectMockKs
    private lateinit var matchmakingService: MatchmakingService


    @Test
    fun `should reject request if end time is before start time`() {
        val request = QueueRequestDTO(
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 1000.0,
            startTime = OffsetDateTime.now(),
            endTime = OffsetDateTime.now().minusHours(1),
        )

        val result = matchmakingService.isValidRequest(request)

        assertFalse(result)
    }

    @Test
    fun `can store a single player`() {
        val myId = UUID.randomUUID()
        val mockUser = User(displayName = "Nazareno", id = myId, division = 3)

        val request = QueueRequestDTO(
            latitude = 40.4167,
            longitude = -3.7037,
            radiusMeters = 5000.0,
            startTime = OffsetDateTime.now(),
            endTime = OffsetDateTime.now().plusHours(1)
        )
        val searchLocation = geometryFactory.createPoint(
            Coordinate(request.longitude, request.latitude)
        )
        val newMatchmakingTicket = MatchmakingTicket(
            id = UUID.randomUUID(),
            userId = myId,
            startTime = request.startTime,
            endTime = request.endTime,
            targetDivision = mockUser.division,
            searchLocation = searchLocation,
            status = TicketStatus.SEARCHING,
            maxRadiusMeters = request.radiusMeters,
        )


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
            address = "Central Padel address"
        )

        every {
            clubRepository.findClubsInIntersection(p1Loc, radius, p2Loc, radius)
        } returns listOf(expectedClub)

        val result = matchmakingService.getClubsForMatchmaking(p1Loc, p2Loc, radius, radius)

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Central Padel", result[0].name)

        verify(exactly = 1) {
            clubRepository.findClubsInIntersection(p1Loc, radius, p2Loc, radius)
        }
    }
}