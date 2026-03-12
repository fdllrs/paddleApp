package com.paddle.app.engine

import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.repository.MatchmakingTicketRepository
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import org.locationtech.jts.geom.Point
import org.slf4j.LoggerFactory
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
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(MatchmakingEngine::class.java)

    @Scheduled(fixedDelay = 10000)
    fun processQueue() {
        logger.info("Matchmaking Engine: Sweeping the queue...")

        try {
            val openTickets = ticketRepository.findByStatusOrderByCreatedAtAsc("SEARCHING")

            for (ticket in openTickets) {

                if (ticket.isExpired(OffsetDateTime.now(clock))) {
                    handleExpiredTicket(ticket)
                    continue
                }

                val searchLocation = ticket.searchLocation
                val maxRadiusMeters = ticket.maxRadiusMeters
                val targetDivision = ticket.targetDivision
                val userId = ticket.userId

                matchService.getNearbyOpenMatches(searchLocation.y, searchLocation.x, maxRadiusMeters, targetDivision)
                    .forEach { match ->
                        val matchId = requireNotNull(match.id)
                        if (matchService.numberOfPlayersInMatch(matchId) == 3) {
                            matchService.joinMatch(matchId, userId)
                            matchmakingService.leaveQueue(userId, MatchmakingService.STATUS_MATCHED)
                            logger.info("Matchmaking Engine: Match found and joined: $matchId")
                            return@forEach
                        }
                    }
            }

        } catch (e: Exception) {
            // If one match calculation crashes, we catch it here so the engine
            // doesn't die. It will wake up again in 10 seconds.
            logger.error("Matchmaking Engine encountered an error: ${e.message}", e)
        }
    }
    private fun handleExpiredTicket(ticket: MatchmakingTicket) {
        matchmakingService.leaveQueue(ticket.userId, MatchmakingService.STATUS_EXPIRED)
        logger.info("Matchmaking Engine: Ticket ${ticket.id} expired (less than 30m remaining or past endTime)")
    }



}