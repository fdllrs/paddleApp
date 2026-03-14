package com.paddle.app.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class MatchCreateRequestDTO(
    val hostId: UUID,
    val courtId: UUID,
    val matchDate: OffsetDateTime,
    val targetDivision: Int,
    val durationMinutes: Int = 90,
    val pricePerPerson: BigDecimal
)