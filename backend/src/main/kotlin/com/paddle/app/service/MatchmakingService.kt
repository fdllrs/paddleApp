package com.paddle.app.service

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.dto.QueueStatusResponseDTO
import com.paddle.app.dto.toQueueStatusResponseDTO
import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import com.paddle.app.repository.MatchmakingTicketRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.stereotype.Service
import java.util.*


@Service
class MatchmakingService(
    private val matchmakingTicketRepository: MatchmakingTicketRepository,
    private val geometryFactory: GeometryFactory,
    private val userService: UserService,
) {

    companion object {

        const val USER_NOT_IN_QUEUE_MESSAGE = "User is not in the matchmaking queue"
        const val USER_ALREADY_IN_QUEUE_MESSAGE = "User is already in the matchmaking queue"
        const val INVALID_TIME_RANGE_MESSAGE = "The provided queue request is not valid: End time must be after start time."

    }


    fun isValidRequestTimeRange(request: QueueRequestDTO): Boolean {
        return request.endTime.isAfter(request.startTime)
    }

    fun joinQueue(request: QueueRequestDTO, userId: UUID): UUID {

        assertQueueJoiningIsValid(userId, request)
        val user = userService.getUserById(userId)


        val searchLocation = geometryFactory.createPoint(
            Coordinate(request.longitude, request.latitude)
        )

        val newMatchmakingTicket = MatchmakingTicket(
            userId = userId,
            targetDivision = user.division,
            searchLocation = searchLocation,
            maxRadiusMeters = request.radiusMeters,
            startTime = request.startTime,
            endTime = request.endTime,
            status = TicketStatus.SEARCHING,
            preferredClubId = request.preferredClubId,
            preferredCourtId = request.preferredCourtId,
            preferredMatchDate = request.preferredDate,
            preferredDurationMinutes = request.preferredDurationMinutes,

        )

        val savedTicket = matchmakingTicketRepository.save(newMatchmakingTicket)

        return requireNotNull(savedTicket.id)
    }

    fun leaveQueueWithStatus(userId: UUID, status: TicketStatus) {
        val ticket = findSearchingTicketForUser(userId)

        ticket.status = status

        matchmakingTicketRepository.save(ticket)
    }

    private fun findSearchingTicketForUser(userId: UUID): MatchmakingTicket {
        return matchmakingTicketRepository.findByUserIdAndStatusIn(
            userId,
            listOf(TicketStatus.SEARCHING, TicketStatus.PROCESSING)) ?:
            throw IllegalArgumentException(USER_NOT_IN_QUEUE_MESSAGE)
    }
    fun isPlayerInQueue(playerID: UUID): Boolean {
        return matchmakingTicketRepository.existsByUserId(playerID)
    }

    fun getTicketStatusForUser(playerID: UUID): QueueStatusResponseDTO {
        val ticket = matchmakingTicketRepository.findByUserId(playerID) ?:
        throw IllegalArgumentException(USER_NOT_IN_QUEUE_MESSAGE)

        return ticket.toQueueStatusResponseDTO()
    }



    fun queueIsEmpty(): Boolean {
        return matchmakingTicketRepository.count() == 0L
    }


    private fun assertQueueJoiningIsValid(userId: UUID, request: QueueRequestDTO) {
        val existingTicket = matchmakingTicketRepository.findByUserIdAndStatusIn(
            userId,
            listOf(TicketStatus.SEARCHING, TicketStatus.PROCESSING)
        )
        if (existingTicket != null) {
            throw IllegalStateException(USER_ALREADY_IN_QUEUE_MESSAGE)
        }

        if (!isValidRequestTimeRange(request)) {
            throw IllegalArgumentException(INVALID_TIME_RANGE_MESSAGE)
        }
    }
}
