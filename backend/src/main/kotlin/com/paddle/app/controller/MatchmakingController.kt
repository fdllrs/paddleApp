package com.paddle.app.controller

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.dto.QueueStatusResponseDTO
import com.paddle.app.model.TicketStatus
import com.paddle.app.service.MatchmakingService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import com.paddle.app.dto.toQueueStatusResponseDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/matchmaking")
class MatchmakingController(private val matchmakingService: MatchmakingService) {

    @PostMapping("/queue")
    fun joinQueue(@RequestParam userId: UUID, @RequestBody request: QueueRequestDTO): ResponseEntity<UUID> {
        val ticketId = matchmakingService.joinQueue(request, userId)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ticketId)
    }

    @GetMapping("/queue/status")
    fun getQueueStatus(@RequestParam userId: UUID): ResponseEntity<QueueStatusResponseDTO> {
        val ticketStatus = matchmakingService.getTicketStatusForUser(userId)
        return ResponseEntity.ok(ticketStatus)
    }

    @DeleteMapping("/queue")
    fun leaveQueue(@RequestParam userId: UUID): ResponseEntity<Void>{
        matchmakingService.leaveQueue(userId, TicketStatus.CANCELLED)
        return ResponseEntity.noContent().build()
    }
}