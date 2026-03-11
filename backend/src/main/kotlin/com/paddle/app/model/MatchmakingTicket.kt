package com.paddle.app.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID
import org.locationtech.jts.geom.Point


@Entity
@Table(name = "matchmaking_tickets")
class MatchmakingTicket (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id")
    var userId: UUID,

    @Column(name = "target_division", nullable = false)
    val targetDivision: Int,

    @Column(name = "search_location", nullable = false)
    val searchLocation: Point,

    @Column(name = "max_radius_meters", nullable = false)
    val maxRadiusMeters: Double,

    @Column(name = "start_time", nullable = false)
    val startTime: OffsetDateTime,

    @Column(name = "end_time", nullable = false)
    val endTime: OffsetDateTime,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "matched_match_id")
    var matchedMatchId: UUID? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    )