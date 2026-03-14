package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID



@Entity
@Table(name = "matches")
class Match (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host", nullable = false)
    var host: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court", nullable = false)
    var court: Court,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MatchStatus = MatchStatus.OPEN,

    @Column(name = "start_date", nullable = false)
    var matchDate: OffsetDateTime,

    @Column(name = "target_division", nullable = false)
    var targetDivision: Int,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = 90,

    @Column(name = "price_per_person", nullable = false, precision = 10, scale = 2)
    var pricePerPerson: BigDecimal,


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
) {
    private fun markAs(status: MatchStatus) {
        this.status = status
    }

    fun isOpen() = this.status == MatchStatus.OPEN
    fun isFull() = this.status == MatchStatus.FULL
    fun isCancelled() = this.status == MatchStatus.CANCELLED
    fun isPlayed() = this.status == MatchStatus.PLAYED

    fun markAsCancelled() = markAs(MatchStatus.CANCELLED)
    fun markAsFull() = markAs(MatchStatus.FULL)
    fun markAsPlayed() = markAs(MatchStatus.PLAYED)
    fun markAsOpen() = markAs(MatchStatus.OPEN)

    fun isHost(userId: UUID) = this.host.id == userId

}

enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    PLAYED
}