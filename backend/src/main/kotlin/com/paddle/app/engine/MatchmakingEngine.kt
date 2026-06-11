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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.OffsetDateTime
import java.util.*

@Component
class MatchmakingEngine(
    private val ticketRepository: MatchmakingTicketRepository,
    private val matchService: MatchService,
    private val matchmakingService: MatchmakingService,
    private val courtRepository: CourtRepository,
    private val clock: Clock,
    private val transactionTemplate: TransactionTemplate

) {
    private enum class ProcessingOutcome {
        MATCHED,
        EXPIRED,
        FAILED
    }

    private val logger = LoggerFactory.getLogger(MatchmakingEngine::class.java)

    @Scheduled(fixedDelayString = $$"${app.matchmaking.fixed-delay-ms:10000}")
    fun processQueue() {
        val cycleStartedAt = OffsetDateTime.now(clock)

        logger.info("Matchmaking Engine: starting queue processing cycle at {}", cycleStartedAt)

        val claimedTickets = claimNextTickets()

        logger.info("Matchmaking Engine: claimed {} ticket(s) for processing", claimedTickets.size)
            var matchedCount = 0
            var expiredCount = 0
            var failedCount = 0

            for (ticket in claimedTickets) {
                try {
                    val outcome = processTicket(ticket)

                    when (outcome) {
                        ProcessingOutcome.MATCHED -> matchedCount++
                        ProcessingOutcome.EXPIRED -> expiredCount++
                        ProcessingOutcome.FAILED -> failedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                    logger.error("Matchmaking Engine: failed processing ticket ${ticket.id}: ${e.message}", e)
                    markTicketAsFailed(ticket)
                }
            }

            logger.info(
                "Matchmaking Engine: completed cycle. claimed={}, matched={}, expired={}, failed={}",
                claimedTickets.size,
                matchedCount,
                expiredCount,
                failedCount
            )
        }

    fun claimNextTickets(): List<MatchmakingTicket> {
        return transactionTemplate.execute {
            val tickets = ticketRepository.findNextTicketsForProcessing(
                TicketStatus.SEARCHING,
                PageRequest.of(0, 25)
            )

            tickets.forEach { ticket ->
                ticket.status = TicketStatus.PROCESSING
            }

            ticketRepository.saveAll(tickets)
        }
    }

    private fun processTicket(ticket: MatchmakingTicket): ProcessingOutcome {
        if (ticket.status != TicketStatus.PROCESSING) {
            logger.warn(
                "Matchmaking Engine: skipping ticket {} because status is {}",
                ticket.id,
                ticket.status
            )
            return ProcessingOutcome.FAILED
        }

        if (ticket.isExpired(OffsetDateTime.now(clock))) {
            handleExpiredTicket(ticket)
            return ProcessingOutcome.EXPIRED
        }

        var joinedMatch = false
        var nearbyMatches: Page<MatchResponseDTO>
        val userId = ticket.userId
        var currentPage = 0

        do {
            val pageable = PageRequest.of(currentPage, 50)
            nearbyMatches = obtainNearbyMatchesFromTicket(ticket, pageable)

            if (tryJoinExistingMatch(nearbyMatches.content, ticket)) {
                joinedMatch = true
                break
            }

            currentPage++
        } while (nearbyMatches.hasNext())

        if (!joinedMatch) {
            createFallbackMatch(ticket, userId)
        }

        return ProcessingOutcome.MATCHED
    }

    private fun markTicketAsFailed(ticket: MatchmakingTicket) {
        ticket.status = TicketStatus.FAILED
        ticketRepository.save(ticket)
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
        matchmakingService.leaveQueueWithStatus(userId, TicketStatus.MATCHED)

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

    private fun obtainNearbyMatchesFromTicket(ticket: MatchmakingTicket, pageable: Pageable): Page<MatchResponseDTO> {
        val searchLocation = ticket.searchLocation
        val maxRadiusMeters = ticket.maxRadiusMeters
        val targetDivision = ticket.targetDivision
        val nearbyMatches = matchService.getNearbyOpenMatches(searchLocation.y, searchLocation.x, maxRadiusMeters,
            targetDivision, pageable)
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
                matchmakingService.leaveQueueWithStatus(userId, TicketStatus.MATCHED)
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

            matchmakingService.leaveQueueWithStatus(userId, TicketStatus.MATCHED)
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
            startDate = ticket.preferredMatchDate,
            targetDivision = ticket.targetDivision,
            durationMinutes = ticket.preferredDurationMinutes,
            pricePerPerson = court.pricePerTurn.div(4.toBigDecimal())
        )
        return request
    }

    private fun handleExpiredTicket(ticket: MatchmakingTicket) {
        matchmakingService.leaveQueueWithStatus(ticket.userId, TicketStatus.EXPIRED)
        logger.info("Matchmaking Engine: Ticket ${ticket.id} expired (less than 30m remaining or past endTime)")
    }
}