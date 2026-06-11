package com.paddle.app.repository

import com.paddle.app.model.MatchmakingTicket
import com.paddle.app.model.TicketStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime
import java.util.*

interface MatchmakingTicketRepository : JpaRepository<MatchmakingTicket, UUID> {

    // 1. The Engine's Primary Query (FIFO Queue)
    fun findByStatusOrderByCreatedAtAsc(status: TicketStatus): List<MatchmakingTicket>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select t
        from MatchmakingTicket t
        where t.status = :status
        order by t.createdAt asc
        """
    )
    fun findNextTicketsForProcessing(status: TicketStatus, pageable: Pageable): List<MatchmakingTicket>


    // 2. The Cleanup Query (Timeouts)
    fun findByStatusAndEndTimeBefore(status: TicketStatus, time: OffsetDateTime): List<MatchmakingTicket>

    // 3. The Anti-Spam Guard
    fun findByUserIdAndStatus(userId: UUID, status: TicketStatus): MatchmakingTicket?

    fun findByUserIdAndStatusIn(userId: UUID, statuses: Collection<TicketStatus>): MatchmakingTicket?

    fun findByUserId(userId: UUID): MatchmakingTicket?

    fun existsByUserId(userId: UUID): Boolean
}