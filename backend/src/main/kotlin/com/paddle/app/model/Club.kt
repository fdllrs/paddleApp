package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.locationtech.jts.geom.Point
import java.time.OffsetDateTime
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

)