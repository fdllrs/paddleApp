package com.paddle.app.dto

import com.paddle.app.model.Match
import com.paddle.app.model.MatchStatus
import java.time.OffsetDateTime
import java.util.UUID

data class MatchResponseDTO(
    val id: UUID?,
    val status: MatchStatus,
    val matchDate: OffsetDateTime?,
    val durationMinutes: Int,
    val pricePerPerson: Float,
    val targetDivision: Int,
    val hostId: UUID?,
    val hostName: String,
    val clubId: UUID?,
    val clubName: String,
    val latitude: Double,
    val longitude: Double
) {


}

fun Match.toResponseDTO(): MatchResponseDTO {

    return MatchResponseDTO(
        id = this.id,
        status = this.status,
        matchDate = this.matchDate,
        durationMinutes = this.durationMinutes,
        pricePerPerson = this.pricePerPerson,
        hostId = this.host.id,
        targetDivision = this.targetDivision,
        hostName = this.host.displayName,
        clubId = this.club.id,
        clubName = this.club.name,
        latitude = this.club.location.y,
        longitude = this.club.location.x
    )
}

