package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.locationtech.jts.geom.Point
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.UUID

@Entity
@Table(name = "clubs")
class Club (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "address", nullable = false, length = 255)
    var address: String,

    @Column(columnDefinition = "geography(Point, 4326)", name = "coordinates", nullable = false)
    var location: Point,


    @Column(name = "open_time", nullable = false)
    var openTime: OffsetTime,

    @Column(name = "close_time", nullable = false)
    var closeTime: OffsetTime,

    @Column(name = "neighborhood", nullable = false, length = 50)
    var neighborhood: String,

    @Column(name = "turn_duration_minutes", nullable = false)
    val turnDurationMinutes: Int = 90,

    @OneToMany(mappedBy = "club", cascade = [CascadeType.ALL], orphanRemoval = true)
    var courts: MutableList<Court> = mutableListOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

) {
    fun addCourt(court: Court){
        this.courts.add(court)
    }
    fun removeCourt(court: Court){
        this.courts.remove(court)
    }

}