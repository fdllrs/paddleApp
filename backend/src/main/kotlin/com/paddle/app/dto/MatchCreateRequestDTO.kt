package com.paddle.app.dto

import java.time.OffsetDateTime
import java.util.UUID

data class MatchCreateRequestDTO(
    val hostId: UUID,
    val clubId: UUID,
    val matchDate: OffsetDateTime,
    val targetDivision: Int,
    val durationMinutes: Int = 90,
    val pricePerPerson: Float
)