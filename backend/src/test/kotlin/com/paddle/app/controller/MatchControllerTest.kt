package com.paddle.app.controller

import com.ninjasquad.springmockk.MockkBean
import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.model.MatchStatus
import com.paddle.app.model.User
import com.paddle.app.repository.UserRepository
import com.paddle.app.security.SecurityConfig
import com.paddle.app.service.MatchService
import com.paddle.app.service.MatchmakingService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.*
import java.util.*

@Import(SecurityConfig::class)
@WebMvcTest(MatchController::class)
class MatchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var matchService: MatchService
    @MockkBean
    private lateinit var matchmakingService: MatchmakingService
    @MockkBean
    private lateinit var userRepository: UserRepository



    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-12T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val fixedDateTime: OffsetDateTime = clock.instant().atOffset(ZoneOffset.UTC)

    private fun testMatchRequestDTO(): MatchCreateRequestDTO = MatchCreateRequestDTO(
        hostId = UUID.randomUUID(),
        courtId = UUID.randomUUID(),
        startDate = fixedDateTime.plusDays(1),
        targetDivision = 4,
        durationMinutes = 90,
        pricePerPerson = BigDecimal("15.00")
    )

    private fun testMatchResponseDTO(dummyMatchId: UUID?, requestDto: MatchCreateRequestDTO): MatchResponseDTO =
        MatchResponseDTO(
        id = dummyMatchId,
        status = MatchStatus.OPEN,
        matchDate = requestDto.startDate,
        durationMinutes = requestDto.durationMinutes,
        pricePerPerson = requestDto.pricePerPerson,
        targetDivision = requestDto.targetDivision,
        hostId = requestDto.hostId,
        hostName = "Test Host",
        courtId = requestDto.courtId,
        courtName = "Test Court",
        clubId = UUID.randomUUID(),
        clubName = "Test Club",
        latitude = -34.6037,
        longitude = -58.3816
    )

    private fun authenticatedUser(
        id: UUID = UUID.randomUUID(),
        firebaseUid: String = "test-firebase-uid"
    ): User =
        User(
            id = id,
            displayName = "Test User",
            division = 5,
            firebaseUid = firebaseUid
        )

    @Test
    fun `GET nearby matches returns 200 OK with paginated content`() {
        // --- ARRANGE ---
        val dummyMatchId = UUID.randomUUID()

        val dummyMatchDto = testMatchResponseDTO(dummyMatchId, testMatchRequestDTO())
        // 1. The Wrapper: Simulating Spring Data's pagination metadata
        val pagedResponse = PageImpl(listOf(dummyMatchDto))
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        every {
            matchService.getNearbyOpenMatches(
                latitude = dummyMatchDto.latitude,
                longitude = dummyMatchDto.longitude,
                radiusMeters = 5000.0,
                targetDivision = dummyMatchDto.targetDivision,
                page = any() // Proving the framework injects the Pageable interface
            )
        } returns pagedResponse

        // --- ACT & ASSERT ---
        mockMvc.perform(
            get("/api/matches/nearby")
                .param("latitude", dummyMatchDto.latitude.toString())
                .param("longitude", dummyMatchDto.longitude.toString())
                .param("targetDivision", dummyMatchDto.targetDivision.toString())
                .param("radiusMeters", "5000.0")
                // 2. The Framework Request Parameters
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))

        )
            .andExpect(status().isOk)

            // 4. The JSON Serialization Contract
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].id").value(dummyMatchId.toString()))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))

        verify { matchService.getNearbyOpenMatches(any(), any(), any(), any(), any()) }

    }

    @Test
    fun `POST match deserializes JSON payload and returns matched DTO`() {
        // --- ARRANGE ---
        val dummyMatchId = UUID.randomUUID()
        val requestDto = testMatchRequestDTO()
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        val expectedResponse = testMatchResponseDTO(dummyMatchId, requestDto)

        // Using exact object matching to prove the controller doesn't mutate the payload
        every { matchService.createMatch(requestDto) } returns expectedResponse

        // --- ACT & ASSERT ---
        mockMvc.perform(
            post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))

        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(dummyMatchId.toString()))
            .andExpect(jsonPath("$.clubName").value(expectedResponse.clubName))

        verify { matchService.createMatch(requestDto) }

    }

    @Test
    fun `POST join match extracts AuthenticationPrincipal and routes correctly`() {
        // --- ARRANGE ---
        val matchId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        every { matchService.joinMatch(matchId, userId) } just Runs


        // --- ACT & ASSERT ---
        mockMvc.perform(
            post("/api/matches/{matchId}/join", matchId)
                .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))
        )
                .andExpect(status().isNoContent)

        verify { matchService.joinMatch(matchId, userId) }

    }

    @Test
    fun `GET match players returns flat JSON array of UserResponseDTOs`() {
        // --- ARRANGE ---
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)
        val matchId = UUID.randomUUID()
        val dummyUserDto = UserResponseDTO(
            id = userId,
            displayName = user.displayName,
            division = 5
        )

        every { matchService.getPlayersFromMatch(matchId) } returns listOf(dummyUserDto)

        // --- ACT & ASSERT ---
        mockMvc.perform(
            get("/api/matches/{matchId}/players", matchId)
            .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].displayName").value(dummyUserDto.displayName))

        verify { matchService.getPlayersFromMatch(matchId) }
    }

    @Test
    fun `GET player matches returns flat JSON array of MatchResponseDTOs`() {
        // --- ARRANGE ---
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        val dummyMatchDto = testMatchResponseDTO(userId, testMatchRequestDTO())

        every { matchService.getMatchesForPlayer(userId) } returns listOf(dummyMatchDto)

        // --- ACT & ASSERT ---
        mockMvc.perform(
            get("/api/matches/my-matches")
            .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].clubName").value(dummyMatchDto.clubName))

        verify { matchService.getMatchesForPlayer(userId) }

    }

    @Test
    fun `POST leave match returns 204 with noContent`() {
        // --- ARRANGE ---
        val matchId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        every { matchService.leaveMatch(matchId, userId) } just Runs


        // --- ACT & ASSERT ---
        mockMvc.perform(
            post("/api/matches/{matchId}/leave", matchId)
                .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))
        )
            .andExpect(status().isNoContent)

        verify { matchService.leaveMatch(matchId, userId) }
    }

    @Test
    fun `POST cancel match returns 204 with noContent`() {
        // --- ARRANGE ---
        val matchId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val user = authenticatedUser(id = userId)

        every { matchService.cancelMatch(matchId, userId) } just Runs


        // --- ACT & ASSERT ---
        mockMvc.perform(
            post("/api/matches/{matchId}/cancel", matchId)
                .with(authentication(UsernamePasswordAuthenticationToken(user, user.firebaseUid, emptyList())))
        )
            .andExpect(status().isNoContent)

        verify { matchService.cancelMatch(matchId, userId) }

    }

    @Test
    fun `GET my-matches returns 403 FORBIDDEN for non-onboarded user`() {
        mockMvc.perform(
            get("/api/matches/my-matches")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().jwt { it.subject("some-firebase-uid") })
        )
            .andExpect(status().isForbidden)
    }
}