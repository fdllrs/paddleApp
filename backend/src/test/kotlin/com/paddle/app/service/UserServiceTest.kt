package com.paddle.app.service

import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.model.User
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {
    @MockK
    private lateinit var matchRepository: MatchRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @InjectMockKs
    private lateinit var userService: UserService

    private fun testUser(
        id: UUID = UUID.randomUUID(),
        division: Int = 5
    ): User =
        User(
            id = id,
            displayName = "Test User",
            division = division,
            firebaseUid = "1234"
        )
    @Test
    fun `Onboarding a user saves it to the database`(){

        val user = testUser()
        val userRequest = UserOnboardRequestDTO(user.displayName, user.division)


        every { userRepository.save(any()) } returns mockk()
        every { userRepository.findUserByFirebaseUid(any()) } returns null


        userService.onboardUser(userRequest, user.firebaseUid)

        // Assert
        verify(exactly = 1) {
            userRepository.save(
                withArg { savedUser ->
                    assertEquals(savedUser.firebaseUid, user.firebaseUid)
                    assertEquals(savedUser.displayName, user.displayName)
                    assertEquals(savedUser.division, user.division)
                }
            )
        }
    }

    @Test
    fun `User cannot be onboarded twice`() {

        val user = testUser()
        val userRequest = UserOnboardRequestDTO(user.displayName, user.division)


        every { userRepository.findUserByFirebaseUid(any()) } returns user

        // Assert
        val exception = assertThrows<IllegalStateException> {
            userService.onboardUser(userRequest, user.firebaseUid)
        }

        // Assert
        assertEquals(UserService.USER_ALREADY_EXISTS_MESSAGE, exception.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}