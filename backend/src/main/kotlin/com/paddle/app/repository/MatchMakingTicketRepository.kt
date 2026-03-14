package com.paddle.app.repository

import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface MatchmakingTicketRepository : JpaRepository<MatchmakingTicket, UUID> {

    // 1. The Engine's Primary Query (FIFO Queue)
    fun findByStatusOrderByCreatedAtAsc(status: TicketStatus): List<MatchmakingTicket>

    // 2. The Cleanup Query (Timeouts)
    fun findByStatusAndEndTimeBefore(status: String, time: OffsetDateTime): List<MatchmakingTicket>

    // 3. The Anti-Spam Guard
    fun findByUserIdAndStatus(userId: UUID, status: String): MatchmakingTicket?

    fun findByUserId(userId: UUID): MatchmakingTicket?

    fun existsByUserId(userId: UUID): Boolean
}