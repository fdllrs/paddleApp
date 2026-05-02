package com.paddle.app.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserOnboardRequestDTO (
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    val displayName: String,

    @field:Min(1)
    @field:Max(7)
    val division: Int
)