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
                if (ticket.isExpired(OffsetDateTime.now(clock))) { handleExpiredTicket(ticket); continue }

                val nearbyMatches = obtainNearbyMatchesFromTicket(ticket)
                val userId = ticket.userId

                if (tryJoinExistingMatch(nearbyMatches, ticket)) continue
                createFallbackMatch(ticket, userId)
            }
        } catch (e: Exception) {
            logger.error("Matchmaking Engine encountered an error: ${e.message}", e)
        }
    }

    private fun tryJoinExistingMatch(
        nearbyMatches: List<MatchResponseDTO>,
        ticket: MatchmakingTicket,
    ): Boolean {
        val userId = ticket.userId
        if (nearbyMatches.isEmpty()) return false

        if (ticket.isSoloQ()) {
            return (attemptToJoinMatch(nearbyMatches, userId))
        }

        val duoCompatibleMatches = matchService.filterDuoCompatibleMatches(nearbyMatches)
        if (duoCompatibleMatches.isEmpty()) return false

        val partnerId = ticket.partnerId!!
        return attemptToJoinDuoMatch(duoCompatibleMatches, userId, partnerId)
    }

    private fun createFallbackMatch(ticket: MatchmakingTicket, userId: UUID) {
        val matchResponseDTO = createDTOFromPreference(ticket)
        matchmakingService.leaveQueue(userId, TicketStatus.MATCHED)

        if (!ticket.isSoloQ()) {
            matchService.joinMatch(matchResponseDTO.id!!, ticket.partnerId!!)
            logger.info("Matchmaking Engine: User $userId created match ${matchResponseDTO.id} and ${ticket.partnerId} joined")
        } else {
            logger.info("Matchmaking Engine: User $userId created match ${matchResponseDTO.id}")
        }
    }

    private fun createDTOFromPreference(ticket: MatchmakingTicket): MatchResponseDTO {
        val matchRequest = createMatchRequestFromPreferences(ticket)
        val matchResponseDTO = matchService.createMatch(matchRequest)
        return matchResponseDTO
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
    ): Boolean {
        for (match in nearbyMatches) {
            val matchId = requireNotNull(match.id)
            try {
                matchService.joinMatch(matchId, userId)
                matchmakingService.leaveQueue(userId, TicketStatus.MATCHED)
                logger.info("Matchmaking Engine: User $userId joined match $matchId")

                return true

            } catch (e: Exception) {
                logger.warn("Failed to join match $matchId: ${e.message}")
            }
        }
        return false
    }

    private fun attemptToJoinDuoMatch(
        duoCompatibleMatches: List<MatchResponseDTO>,
        userId: UUID,
        partnerId: UUID
    ): Boolean {
        for (match in duoCompatibleMatches) {
            val matchId = requireNotNull(match.id)

            try {
                matchService.joinMatch(matchId, userId)
            } catch (e: Exception) {
                logger.warn("First player in duo failed to join match $matchId: ${e.message}")
                continue
            }

            try {
                matchService.joinMatch(matchId, partnerId)
            } catch (e: Exception) {
                logger.warn("Second player in duo failed to join match $matchId: ${e.message}. Rolling back.")
                matchService.leaveMatch(matchId, userId)
                continue
            }

            matchmakingService.leaveQueue(userId, TicketStatus.MATCHED)
            logger.info("Matchmaking Engine: Duo ($userId, $partnerId) joined match $matchId")

            return true
        }

        return false
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