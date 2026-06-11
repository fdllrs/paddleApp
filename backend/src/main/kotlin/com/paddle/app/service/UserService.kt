package com.paddle.app.service

import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.User
import com.paddle.app.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService (
    private val userRepository: UserRepository
) {

    companion object {
        const val USER_ALREADY_EXISTS_MESSAGE = "User already exists"
        const val USER_NOT_FOUND_MESSAGE = "User not found"
    }

    fun getUserByFirebaseUid(firebaseUid: String): UserResponseDTO? {
        val user = userRepository.findUserByFirebaseUid(firebaseUid) ?: return null

        return user.toResponseDTO()

    }

    @Transactional
    fun onboardUser(userRequest: UserOnboardRequestDTO, firebaseUid: String)  {

        assertUserNotAlreadyOnboarded(firebaseUid)

        val user = User(
            displayName = userRequest.displayName,
            division = userRequest.division,
            firebaseUid = firebaseUid)

        userRepository.save(user)
    }

    fun getUserById(userId: UUID): User {
        return userRepository.findUserById(userId) ?: throw IllegalArgumentException(USER_NOT_FOUND_MESSAGE)
    }

    private fun assertUserNotAlreadyOnboarded(firebaseUid: String) {
        if (getUserByFirebaseUid(firebaseUid) != null) throw IllegalStateException(USER_ALREADY_EXISTS_MESSAGE)
    }

}