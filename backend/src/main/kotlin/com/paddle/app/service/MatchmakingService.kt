package com.paddle.app.service

import com.paddle.app.dto.QueueRequestDTO
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MatchmakingService {
    private val activeQueue = mutableListOf<QueueRequestDTO>()

    fun isValidRequest(request: QueueRequestDTO): Boolean {
        return request.endTime.isAfter(request.startTime)
    }

    fun enterQueue(request: QueueRequestDTO) {
        if (isValidRequest(request)) {
            activeQueue.add(request)
        }
    }

    fun isPlayerInQueue(playerID: UUID): Boolean {
        return activeQueue.any { it.playerId == playerID }
    }

    fun queueIsEmpty(): Boolean {
        return activeQueue.isEmpty()
    }
}