package com.paddle.app.service

import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.User
import com.paddle.app.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService (
    private val userRepository: UserRepository,
) {



    fun getUser(firebaseUid: String): UserResponseDTO? {
        val user = userRepository.findUserByFirebaseUid(firebaseUid) ?: return null

        return user.toResponseDTO()

    }

    @Transactional
    fun onboardUser(displayName: String, firebaseUid: String, division: Int)  {
        val user = User(displayName = displayName, division = division, firebaseUid = firebaseUid)
        userRepository.save(user)
    }

}