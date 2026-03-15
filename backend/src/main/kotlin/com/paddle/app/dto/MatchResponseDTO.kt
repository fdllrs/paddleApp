package com.paddle.app.dto

import com.paddle.app.model.Match
import com.paddle.app.model.MatchStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class MatchResponseDTO(
    val id: UUID?,
    val status: MatchStatus,
    val matchDate: OffsetDateTime,
    val durationMinutes: Int,
    val pricePerPerson: BigDecimal,
    val targetDivision: Int,
    val hostId: UUID?,
    val hostName: String,
    val courtId: UUID?,
    val courtName: String,
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
        matchDate = this.startDate,
        durationMinutes = this.durationMinutes,
        pricePerPerson = this.pricePerPerson,
        hostId = this.host.id,
        targetDivision = this.targetDivision,
        hostName = this.host.displayName,
        clubId = this.court.club.id,
        clubName = this.court.club.name,
        courtId = this.court.id,
        courtName = this.court.name,
        latitude = this.court.club.location.y,
        longitude = this.court.club.location.x
    )
}

