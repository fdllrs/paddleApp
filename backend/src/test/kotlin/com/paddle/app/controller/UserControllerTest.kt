package com.paddle.app.controller

import com.ninjasquad.springmockk.MockkBean
import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.User
import com.paddle.app.repository.UserRepository
import com.paddle.app.security.SecurityConfig
import com.paddle.app.service.UserService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.*

@Import(SecurityConfig::class)
@WebMvcTest(UserController::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var UserRepository: UserRepository


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
    fun `POST onboard returns 201 CREATED`() {
        val firebaseUid = "firebase-uid-123"
        val request = UserOnboardRequestDTO(
            displayName = "Facundo",
            division = 5
        )

        every {
            userService.onboardUser(
                userRequest = request,
                firebaseUid = firebaseUid
            )
        } just Runs

        mockMvc.perform(
            post("/api/users/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt { it.subject(firebaseUid) })
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST onboard returns 400 BAD_REQUEST if request is invalid`() {
        val firebaseUid = "firebase-uid-123"
        val invalidRequest = UserOnboardRequestDTO(
            displayName = "Facundo",
            division = -5
        )


        mockMvc.perform(
            post("/api/users/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(jwt().jwt { it.subject(firebaseUid) })
        )
            .andExpect(status().isBadRequest)

    }

    @Test
    fun `GET user returns 200 OK`() {
        val user = testUser()

        every { userService.getUserByFirebaseUid(user.firebaseUid)} returns user.toResponseDTO()

        mockMvc.perform(
            get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt { it.subject(user.firebaseUid) })
        )
            .andExpect(status().isOk)
            .andExpect(content().string(objectMapper.writeValueAsString(user.toResponseDTO())))

    }

    @Test
    fun `GET user returns 401 UNAUTHORIZED if user is not authenticated`() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET user returns NOT FOUND if user is not found`() {
        val firebaseUid = "firebase-uid-123"

        every { userService.getUserByFirebaseUid(any())} returns null

        mockMvc.perform(
            get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt { it.subject(firebaseUid) })
        )
            .andExpect(status().isNotFound)
    }
}