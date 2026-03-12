package com.paddle.app.service

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.model.Club
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.UserRepository
import com.paddle.app.repository.MatchmakingTicketRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

@Service
class MatchmakingService(
    private val userRepository: UserRepository,
    private val matchmakingTicketRepository: MatchmakingTicketRepository,
    private val geometryFactory: GeometryFactory,
    private val clubRepository: ClubRepository
) {
    companion object {
        const val STATUS_SEARCHING = "SEARCHING"
        const val STATUS_CANCELLED = "CANCELLED"
        const val STATUS_EXPIRED = "EXIPRED"
        const val STATUS_MATCHED = "MATCHED"
    }

    fun isValidRequest(request: QueueRequestDTO): Boolean {
        return request.endTime.isAfter(request.startTime)
    }

    fun joinQueue(request: QueueRequestDTO, userId: UUID): UUID {

        assertQueueJoiningIsValid(userId, request)

        val user = userRepository.findUserById(userId)
            ?: throw IllegalArgumentException("User not found")

        val searchLocation = geometryFactory.createPoint(
            Coordinate(request.longitude, request.latitude)
        )

        val newMatchmakingTicket = MatchmakingTicket(
            userId = userId,
            startTime = request.startTime,
            endTime = request.endTime,
            targetDivision = user.division,
            searchLocation = searchLocation,
            status = STATUS_SEARCHING,
            maxRadiusMeters = request.radiusMeters,
        )

        val savedTicket = matchmakingTicketRepository.save(newMatchmakingTicket)

        return savedTicket.id!!
    }

    fun leaveQueue(userId: UUID, status: String) {
        val ticket = matchmakingTicketRepository.findByUserIdAndStatus(userId, STATUS_SEARCHING) ?:
        throw IllegalArgumentException("User is not in the matchmaking queue")

        ticket.status = status

        matchmakingTicketRepository.save(ticket)
    }

    fun getClubsForMatchmaking(p1Loc: Point, p2Loc: Point, p1radius: Double, p2radius: Double): List<Club>{
        return clubRepository.findClubsInIntersection(
            p1Loc,
            p1radius,
            p2Loc,
            p2radius
        )
    }

    fun isPlayerInQueue(playerID: UUID): Boolean {
        return matchmakingTicketRepository.existsByUserId(playerID)
    }

    fun queueIsEmpty(): Boolean {
        return matchmakingTicketRepository.count() == 0L
    }

    private fun assertQueueJoiningIsValid(userId: UUID, request: QueueRequestDTO) {
        val existingTicket = matchmakingTicketRepository.findByUserIdAndStatus(userId, STATUS_SEARCHING)
        if (existingTicket != null) {
            throw IllegalStateException("User is already in the matchmaking queue")
        }

        if (!isValidRequest(request)) {
            throw IllegalArgumentException("The provided queue request is not valid: End time must be after start time.")
        }
    }
}
