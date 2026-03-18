package com.paddle.app.engine

import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.data.domain.PageImpl
import java.time.*
import java.util.*

@ExtendWith(MockKExtension::class)
class MatchmakingEngineTest {


    @MockK
    private lateinit var matchmakingTicketRepository: MatchmakingTicketRepository
    @MockK
    private lateinit var matchService: MatchService
    @MockK
    private lateinit var matchmakingService: MatchmakingService
    @MockK
    private lateinit var courtRepository: CourtRepository
    @SpyK
    private var geometryFactory = GeometryFactory()

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val fixedDateTime: OffsetDateTime = clock.instant().atOffset(ZoneOffset.UTC)

    @InjectMockKs
    private lateinit var matchmakingEngine: MatchmakingEngine

    @Test
    fun `queueing player joins a compatible match and ticket is updated`() {
        // Arrange
        val userId = UUID.randomUUID()
        val matchId = UUID.randomUUID()

        val ticket = MatchmakingTicket(
            id = UUID.randomUUID(),
            userId = userId,
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(-3.7038, 40.4168)),
            maxRadiusMeters = 5000.0,
            startTime = fixedDateTime.plusDays(100),
            endTime = fixedDateTime.plusDays(300),
            status = TicketStatus.SEARCHING,
            preferredCourtId = UUID.randomUUID(),
            preferredClubId = UUID.randomUUID(),
            preferredMatchDate = fixedDateTime.plusDays(200),
            preferredDurationMinutes = 90
        )

        val mockMatchDTO = mockk<MatchResponseDTO> {
            every { id } returns matchId
        }
        val pagedResponse = PageImpl(listOf(mockMatchDTO))

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(ticket)

        every {
            matchService.getNearbyOpenMatches(
                ticket.searchLocation.y,
                ticket.searchLocation.x,
                ticket.maxRadiusMeters,
                ticket.targetDivision,
                any()
            )
        } returns pagedResponse

        every { matchService.joinMatch(matchId, userId) } just Runs
        every { matchmakingService.leaveQueue(userId, any()) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) { matchService.joinMatch(matchId, userId) }
        verify(exactly = 1) { matchmakingService.leaveQueue(userId, TicketStatus.MATCHED) }
    }

    @Test
    fun `matchmaking ticket expires if start time is in less than 30 min`() {

        val startTime = fixedDateTime.plusMinutes(25)
        val expiredTicket = MatchmakingTicket(
            userId = UUID.randomUUID(),
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(0.0, 0.0)),
            maxRadiusMeters = 5000.0,
            startTime = startTime,
            endTime = startTime.plusHours(2),
            status = TicketStatus.SEARCHING,
            preferredCourtId = UUID.randomUUID(),
            preferredClubId = UUID.randomUUID(),
            preferredMatchDate = startTime.plusDays(1),
            preferredDurationMinutes = 90
        )

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(expiredTicket)
        every { matchmakingService.leaveQueue(expiredTicket.userId, TicketStatus.EXPIRED) } just Runs

        matchmakingEngine.processQueue()

        // Assert
        verify(exactly = 1) {
            matchmakingService.leaveQueue(expiredTicket.userId, TicketStatus.EXPIRED)
        }

        verify(exactly = 0) {
            matchService.getNearbyOpenMatches(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `engine tries next match if joining the first match fails`() {
        val userId = UUID.randomUUID()
        val matchId1 = UUID.randomUUID()
        val matchId2 = UUID.randomUUID()

        val ticket = MatchmakingTicket(
            userId = userId,
            targetDivision = 4,
            searchLocation = geometryFactory.createPoint(Coordinate(0.0, 0.0)),
            maxRadiusMeters = 5000.0,
            startTime = fixedDateTime.plusDays(1),
            endTime = fixedDateTime.plusDays(2),
            status = TicketStatus.SEARCHING,
            preferredCourtId = UUID.randomUUID(),
            preferredClubId = UUID.randomUUID(),
            preferredMatchDate = fixedDateTime.plusDays(1),
            preferredDurationMinutes = 90
        )

        val match1 = mockk<MatchResponseDTO> { every { id } returns matchId1 }
        val match2 = mockk<MatchResponseDTO> { every { id } returns matchId2 }

        val pagedResponse = PageImpl(listOf(match1, match2))

        every { matchmakingTicketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING) } returns listOf(ticket)
        // Engine finds 2 nearby matches
        every { matchService.getNearbyOpenMatches(any(), any(), any(), any(), any()) } returns pagedResponse

        // match1 throws an exception (e.g., game is full)
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