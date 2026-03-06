package com.paddle.app.dto

import com.paddle.app.model.User
import java.util.UUID

data class UserResponseDTO(
    val id: UUID?,
    val displayName: String,
    val division: Int
)

fun User.toResponseDTO(): UserResponseDTO {
    return UserResponseDTO(
        id = this.id,
        displayName = this.displayName,
        division = this.division
    )
}