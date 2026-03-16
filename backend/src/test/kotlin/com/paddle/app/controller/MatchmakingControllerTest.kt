package com.paddle.app.controller

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.service.MatchmakingService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID
import com.ninjasquad.springmockk.MockkBean
import com.paddle.app.dto.QueueStatusResponseDTO
import com.paddle.app.model.TicketStatus
import io.mockk.Runs
import io.mockk.just
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@WebMvcTest(MatchmakingController::class)
class MatchmakingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var matchmakingService: MatchmakingService

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val fixedDateTime: OffsetDateTime = clock.instant().atOffset(ZoneOffset.UTC)


    private fun testQueueRequestDTO(): QueueRequestDTO = QueueRequestDTO(
        latitude = -34.6037,
        longitude = -58.3816,
        radiusMeters = 5000.0,
        startTime = fixedDateTime.plusHours(1),
        endTime = fixedDateTime.plusHours(3),
        preferredDate = fixedDateTime.plusHours(2),
        preferredDurationMinutes = 90,
        preferredClubId = UUID.randomUUID(),
        preferredCourtId = UUID.randomUUID()
    )

    @Test
    fun `POST queue returns 202 Accepted and the ticket ID`() {
        // --- ARRANGE ---
        val userId = UUID.randomUUID()
        val expectedTicketId = UUID.randomUUID()

        val requestDto = testQueueRequestDTO()

        every { matchmakingService.joinQueue(any(), any()) } returns expectedTicketId

        // --- ACT & ASSERT ---
        mockMvc.perform(
            post("/api/matchmaking/queue")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)) // Converts DTO to JSON
        )
            .andExpect(status().isAccepted) // Asserts HTTP 202
            .andExpect(content().string(objectMapper.writeValueAsString(expectedTicketId))) // Asserts the body contains the UUID
    }



    @Test
    fun `GET queue status returns OK status with ticketId`(){
        // --- ARRANGE ---
        val userId = UUID.randomUUID()
        val expectedTicketStatus = QueueStatusResponseDTO(
            status = TicketStatus.SEARCHING,
            matchId = null
        )

        every { matchmakingService.getTicketStatusForUser(userId) } returns expectedTicketStatus

        // --- ACT & ASSERT ---
        mockMvc.perform(
            get("/api/matchmaking/queue/status")
                .param("userId", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().string(objectMapper.writeValueAsString(expectedTicketStatus)))

    }

    @Test
    fun `DELETE queue returns NoContent`(){
        // --- ARRANGE ---
        val userId = UUID.randomUUID()

        every { matchmakingService.leaveQueue(any(), any()) } just Runs

        // --- ACT & ASSERT ---
        mockMvc.perform(
            delete("/api/matchmaking/queue")
                .param("userId", userId.toString())
        )
            .andExpect(status().isNoContent)


    }
}