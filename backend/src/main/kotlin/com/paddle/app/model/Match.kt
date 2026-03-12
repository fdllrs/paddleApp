package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID



@Entity
@Table(name = "matches")
class Match (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    var host: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club,

    @Column(name = "status", nullable = false)
    var status: MatchStatus = MatchStatus.OPEN,

    @Column(name = "start_date", nullable = false)
    var matchDate: OffsetDateTime? = null,

    @Column(name = "target_division", nullable = false)
    var targetDivision: Int,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = 90,

    @Column(name = "price_per_person", nullable = false)
    var pricePerPerson: Float,


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
) {
    private fun markAs(status: MatchStatus) {
        this.status = status
    }

    fun isOpen(): Boolean {
        return this.status == MatchStatus.OPEN
    }
    fun isFull(): Boolean {
        return this.status == MatchStatus.FULL
    }
    fun isCancelled(): Boolean {
        return this.status == MatchStatus.CANCELLED
    }
    fun isPlayed(): Boolean {
        return this.status == MatchStatus.PLAYED
    }

    fun markAsCancelled() {
        this.markAs(MatchStatus.CANCELLED)
    }
    fun markAsFull() {
        this.markAs(MatchStatus.FULL)
    }
    fun markAsPlayed() {
        this.markAs(MatchStatus.PLAYED)
    }
    fun markAsOpen() {
        this.markAs(MatchStatus.OPEN)
    }

    fun isHost(userId: UUID): Boolean {
        return this.host.id == userId
    }


}

enum class MatchStatus {
    OPEN,
    FULL,
    CANCELLED,
    PLAYED
}