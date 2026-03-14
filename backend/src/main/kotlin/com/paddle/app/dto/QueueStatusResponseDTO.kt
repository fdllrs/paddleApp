package com.paddle.app.dto

import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import java.util.UUID

data class QueueStatusResponseDTO(
    val status: TicketStatus,
    val matchId: UUID? = null
)



fun MatchmakingTicket.toQueueStatusResponseDTO(): QueueStatusResponseDTO {
    return QueueStatusResponseDTO(
        status = this.status,
        matchId = matchedMatchId
    )
}