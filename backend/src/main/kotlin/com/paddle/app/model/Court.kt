package com.paddle.app.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "courts")
class Court (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club", nullable = false)
    val club: Club,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "price_per_turn", nullable = false)
    var pricePerTurn: BigDecimal,

    @Column(name = "covered", nullable = false)
    var covered: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "wall_type", nullable = false)
    var wallType: WallType,

    @Enumerated(EnumType.STRING)
    @Column(name = "floor_type", nullable = false)
    var floorType: FloorType
    )



enum class WallType {
    CEMENT,
    GLASS
}

enum class FloorType {
    CEMENT,
    SYNTHETIC_GRASS
}