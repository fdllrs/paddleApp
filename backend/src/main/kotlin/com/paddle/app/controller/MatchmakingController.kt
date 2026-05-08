package com.paddle.app.controller

import com.paddle.app.dto.QueueRequestDTO
import com.paddle.app.dto.QueueStatusResponseDTO
import com.paddle.app.model.TicketStatus
import com.paddle.app.model.User
import com.paddle.app.service.MatchmakingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/matchmaking")
class MatchmakingController(private val matchmakingService: MatchmakingService) {

    @PostMapping("/queue")
    fun joinQueue(@AuthenticationPrincipal user: User, @RequestBody request: QueueRequestDTO): ResponseEntity<UUID> {
        val ticketId = matchmakingService.joinQueue(request, user.id!!)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ticketId)
    }

    @GetMapping("/queue/status")
    fun getQueueStatus(@AuthenticationPrincipal user: User): ResponseEntity<QueueStatusResponseDTO> {
        val ticketStatus = matchmakingService.getTicketStatusForUser(user.id!!)
        return ResponseEntity.status(HttpStatus.OK).body(ticketStatus)
    }

    @DeleteMapping("/queue")
    fun leaveQueue(@AuthenticationPrincipal user: User): ResponseEntity<Void>{
        matchmakingService.leaveQueueWithStatus(user.id!!, TicketStatus.CANCELLED)
        return ResponseEntity.noContent().build()
    }
}