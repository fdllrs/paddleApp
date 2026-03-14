package com.paddle.app.engine

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Component
class MatchmakingEngine(
    private val ticketRepository: MatchmakingTicketRepository,
    private val matchService: MatchService,
    private val matchmakingService: MatchmakingService,
    private val courtRepository: CourtRepository,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(MatchmakingEngine::class.java)

    @Scheduled(fixedDelay = 10000)
    fun processQueue() {
        logger.info("Matchmaking Engine: Sweeping the queue...")

        try {
            val openTickets = ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.SEARCHING)

            for (ticket in openTickets) {

                if (ticket.isExpired(OffsetDateTime.now(clock))) {
                    handleExpiredTicket(ticket)
                    continue
                }
                val userId = ticket.userId
                val nearbyMatches = obtainNearbyMatchesFromTicket(ticket)
                val userId = ticket.userId

                if (tryJoinExistingMatch(nearbyMatches, ticket)) continue
                createFallbackMatch(ticket, userId)
            }
        } catch (e: Exception) {
            // If one match calculation crashes, we catch it here so the engine
            // doesn't die. It will wake up again in 10 seconds.
            logger.error("Matchmaking Engine encountered an error: ${e.message}", e)
        }
    }

    private fun obtainNearbyMatchesFromTicket(ticket: MatchmakingTicket): List<MatchResponseDTO> {
        val searchLocation = ticket.searchLocation
        val maxRadiusMeters = ticket.maxRadiusMeters
        val targetDivision = ticket.targetDivision
        val nearbyMatches = matchService.getNearbyOpenMatches(searchLocation.y, searchLocation.x, maxRadiusMeters, targetDivision)
        return nearbyMatches
    }

    private fun attemptToJoinMatch(
        nearbyMatches: List<MatchResponseDTO>,
        userId: UUID
    ) {
        for (match in nearbyMatches) {
            val matchId = requireNotNull(match.id)
            try {
                matchService.joinMatch(matchId, userId)
                matchmakingService.leaveQueue(userId, TicketStatus.MATCHED)
                logger.info("Matchmaking Engine: User $userId joined match $matchId")

                break

            } catch (e: Exception) {
                logger.warn("Failed to join match $matchId: ${e.message}")
            }
        }
    }

    private fun createMatchRequestFromPreferences(ticket: MatchmakingTicket): MatchCreateRequestDTO {
        val court = courtRepository.findByIdOrNull(ticket.preferredCourtId) ?: throw IllegalArgumentException("Court not found")

        val request = MatchCreateRequestDTO(
            hostId = ticket.userId,
            courtId = ticket.preferredCourtId,
            matchDate = ticket.preferredMatchDate,
            targetDivision = ticket.targetDivision,
            durationMinutes = ticket.preferredDurationMinutes,
            pricePerPerson = court.pricePerTurn.div(4.toBigDecimal())
        )
        return request
    }

    private fun handleExpiredTicket(ticket: MatchmakingTicket) {
        matchmakingService.leaveQueue(ticket.userId, TicketStatus.EXPIRED)
        logger.info("Matchmaking Engine: Ticket ${ticket.id} expired (less than 30m remaining or past endTime)")
    }



}