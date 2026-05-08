package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point
import java.time.OffsetDateTime
import java.util.*


@Entity
@Table(name = "matchmaking_tickets")
class MatchmakingTicket (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "partner_id")
    var partnerId: UUID? = null,

    @Column(name = "target_division", nullable = false)
    val targetDivision: Int,


    @Column(name = "search_location", columnDefinition = "geography(Point, 4326)", nullable = false)
    val searchLocation: Point,

    @Column(name = "max_radius_meters", nullable = false)
    val maxRadiusMeters: Double,

    @Column(name = "start_time", nullable = false)
    val startTime: OffsetDateTime,

    @Column(name = "end_time", nullable = false)
    val endTime: OffsetDateTime,

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TicketStatus,

    @Column(name = "matched_match_id")
    var matchedMatchId: UUID? = null,

    @Column(name = "preferred_court_id")
    val preferredCourtId: UUID,

    @Column(name = "preferred_club_id")
    val preferredClubId: UUID,

    @Column(name = "preferred_match_date")
    val preferredMatchDate: OffsetDateTime,

    @Column(name = "preferred_duration_minutes")
    val preferredDurationMinutes: Int = 90,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    ) {
    fun isExpired(currentTime: OffsetDateTime): Boolean {
        val expirationThreshold = this.startTime.minusMinutes(30)
        return currentTime.isAfter(expirationThreshold)
    }
    fun markAsCancelled() {
        this.changeStatusTo(TicketStatus.CANCELLED)
    }
    fun markAsExpired() {
        this.changeStatusTo(TicketStatus.EXPIRED)
    }
    fun markAsMatched(matchId: UUID) {
        this.changeStatusTo(TicketStatus.MATCHED)
        this.matchedMatchId = matchId
    }


    private fun changeStatusTo(status: TicketStatus) {
        this.status = status
    }

    fun isSoloQ(): Boolean {
        return this.partnerId == null
    }
}
enum class TicketStatus {
    SEARCHING,
    PROCESSING,
    MATCHED,
    EXPIRED,
    CANCELLED,
    FAILED
}