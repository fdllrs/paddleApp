package com.paddle.app.engine

import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
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

    @InjectMockKs
    private lateinit var matchmakingEngine: MatchmakingEngine

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )

    @Test
    fun `queueing player joins a compatible 3-player match and ticket is updated`() {
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
            status = "SEARCHING"
        )

        val mockMatchDTO = mockk<MatchResponseDTO> {
            every { id } returns matchId
        }

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc("SEARCHING") } returns listOf(ticket)

        every {
            matchService.getNearbyOpenMatches(
                ticket.searchLocation.y,
                ticket.searchLocation.x,
                ticket.maxRadiusMeters,
                ticket.targetDivision
            )
        } returns listOf(mockMatchDTO)

        every { matchService.numberOfPlayersInMatch(matchId) } returns 3
        every { matchService.joinMatch(matchId, userId) } just Runs
        every { matchmakingService.leaveQueue(userId, any()) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) { matchService.joinMatch(matchId, userId) }
        verify(exactly = 1) { matchmakingService.leaveQueue(userId, MatchmakingService.STATUS_MATCHED) }
    }

    @Test
    fun `matchmaking ticket expires if start time is in less than 30 min`() {
        val now = OffsetDateTime.now(fixedClock)

        val startTime = now.plusMinutes(25)
        val expiredTicket = MatchmakingTicket(
            userId = UUID.randomUUID(),
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(0.0, 0.0)),
            maxRadiusMeters = 5000.0,
            startTime = startTime,
            endTime = startTime.plusHours(2),
            status = "SEARCHING"
        )

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc("SEARCHING") } returns listOf(expiredTicket)
        every { matchmakingService.leaveQueue(expiredTicket.userId, MatchmakingService.STATUS_CANCELLED) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) {
            matchmakingService.leaveQueue(expiredTicket.userId, MatchmakingService.STATUS_EXPIRED)
        }

        verify(exactly = 0) {
            matchService.getNearbyOpenMatches(any(), any(), any(), any())
        }
    }
}