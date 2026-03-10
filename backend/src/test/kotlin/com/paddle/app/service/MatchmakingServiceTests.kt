package com.paddle.app.service

import com.paddle.app.dto.QueueRequestDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertEquals

class MatchmakingServiceTest {

    private val service = MatchmakingService()

    @Test
    fun `should reject request if end time is before start time`() {
        val request = QueueRequestDTO(
            playerId = UUID.randomUUID(),
            latitude = 0.0,
            longitude = 0.0,
            targetDivision = 1,
            radiusMeters = 1000.0,
            startTime = OffsetDateTime.now(),
            endTime = OffsetDateTime.now().minusHours(1)
        )

        val result = service.isValidRequest(request)

        assertFalse(result)
    }

    @Test
    fun `can store a single player`() {
        // 1. Setup
        val myId = UUID.randomUUID()
        val request = QueueRequestDTO(
            playerId = myId,
            latitude = 40.4167,
            longitude = -3.7037,
            targetDivision = 3,
            radiusMeters = 5000.0,
            startTime = OffsetDateTime.now(),
            endTime = OffsetDateTime.now().plusHours(1)
        )

        // 2. Action
        service.enterQueue(request)

        // 3. Asserts
        assertFalse(service.queueIsEmpty())
        assertTrue(service.isPlayerInQueue(myId))

    }
}

