package com.paddle.app.dto

import java.time.OffsetDateTime
import java.util.UUID

data class QueueRequestDTO(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)