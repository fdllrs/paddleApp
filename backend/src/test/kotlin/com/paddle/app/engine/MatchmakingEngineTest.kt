package com.paddle.app.engine

import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import io.mockk.impl.annotations.InjectMockKs

@ExtendWith(MockKExtension::class)
class MatchmakingEngineTest {


    @MockK
    private lateinit var matchmakingTicketRepository: MatchmakingTicketRepository
    @MockK
    private lateinit var matchService: MatchService
    @MockK
    private lateinit var matchmakingService: MatchmakingService
    @SpyK
    private var geometryFactory = GeometryFactory()
    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )

    @InjectMockKs
    private lateinit var matchmakingEngine: MatchmakingEngine


    @Test
    fun `queueing player joins a compatible match and ticket is updated`() {
        // Arrange
        val userId = UUID.randomUUID()
        val matchId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val ticket = MatchmakingTicket(
            id = UUID.randomUUID(),
            userId = userId,
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(-3.7038, 40.4168)),
            maxRadiusMeters = 5000.0,
            startTime = now.plusDays(100),
            endTime = now.plusDays(300),
            status = TicketStatus.SEARCHING
        )

        val mockMatchDTO = mockk<MatchResponseDTO> {
            every { id } returns matchId
        }

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(ticket)

        every {
            matchService.getNearbyOpenMatches(
                ticket.searchLocation.y,
                ticket.searchLocation.x,
                ticket.maxRadiusMeters,
                ticket.targetDivision
            )
        } returns listOf(mockMatchDTO)

        every { matchService.joinMatch(matchId, userId) } just Runs
        every { matchmakingService.leaveQueue(userId, any()) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) { matchService.joinMatch(matchId, userId) }
        verify(exactly = 1) { matchmakingService.leaveQueue(userId, TicketStatus.MATCHED) }
    }

    @Test
    fun `matchmaking ticket expires if start time is in less than 30 min`() {
        val now = OffsetDateTime.now(clock)

        val startTime = now.plusMinutes(25)
        val expiredTicket = MatchmakingTicket(
            userId = UUID.randomUUID(),
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(0.0, 0.0)),
            maxRadiusMeters = 5000.0,
            startTime = startTime,
            endTime = startTime.plusHours(2),
            status = TicketStatus.SEARCHING
        )

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(expiredTicket)
        every { matchmakingService.leaveQueue(expiredTicket.userId, TicketStatus.EXPIRED) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) {
            matchmakingService.leaveQueue(expiredTicket.userId, TicketStatus.EXPIRED)
        }

        verify(exactly = 0) {
            matchService.getNearbyOpenMatches(any(), any(), any(), any())
        }
    }

    @Test
    fun `engine tries next match if joining the first match fails`() {
        val userId = UUID.randomUUID()
        val matchId1 = UUID.randomUUID()
        val matchId2 = UUID.randomUUID()
        val now = OffsetDateTime.now(clock)

        val ticket = MatchmakingTicket(
            userId = userId,
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(0.0, 0.0)),
            maxRadiusMeters = 5000.0,
            startTime = now.plusDays(1),
            endTime = now.plusDays(2),
            status = TicketStatus.SEARCHING
        )

        val match1 = mockk<MatchResponseDTO> { every { id } returns matchId1 }
        val match2 = mockk<MatchResponseDTO> { every { id } returns matchId2 }

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(ticket)
        // Engine finds 2 nearby matches
        every { matchService.getNearbyOpenMatches(any(), any(), any(), any()) } returns listOf(match1, match2)

        // match1 throws an exception (e.g. game is full)
        every { matchService.joinMatch(matchId1, userId) } throws IllegalStateException("Match full")
        // match2 succeeds
        every { matchService.joinMatch(matchId2, userId) } just Runs
        every { matchmakingService.leaveQueue(userId, TicketStatus.MATCHED) } just Runs

        // Act
        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) { matchService.joinMatch(matchId1, userId) }
        verify(exactly = 1) { matchService.joinMatch(matchId2, userId) }
        verify(exactly = 1) { matchmakingService.leaveQueue(userId, TicketStatus.MATCHED) }
    }
}