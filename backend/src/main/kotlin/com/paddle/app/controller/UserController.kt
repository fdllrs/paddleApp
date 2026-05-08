package com.paddle.app.controller

import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {


    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<UserResponseDTO> {
        val user = userService.getUserByFirebaseUid(jwt.subject) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.status(HttpStatus.OK).body(user)
    }


    @PostMapping("/onboard")
    fun onboardUser(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody userRequest: UserOnboardRequestDTO):
            ResponseEntity<Void> {
        userService.onboardUser(userRequest = userRequest, firebaseUid =  jwt.subject)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
