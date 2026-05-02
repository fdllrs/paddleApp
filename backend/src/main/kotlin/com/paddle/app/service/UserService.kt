package com.paddle.app.service

import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.User
import com.paddle.app.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService (
    private val userRepository: UserRepository
) {

    companion object {
        const val USER_ALREADY_EXISTS_MESSAGE = "User already exists"
    }

    fun getUser(firebaseUid: String): UserResponseDTO? {
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






    private fun assertUserNotAlreadyOnboarded(firebaseUid: String) {
        if (this.getUser(firebaseUid) != null) throw IllegalStateException(USER_ALREADY_EXISTS_MESSAGE)
    }

}