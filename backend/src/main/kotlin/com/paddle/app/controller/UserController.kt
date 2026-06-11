package com.paddle.app.controller

import com.paddle.app.dto.UserOnboardRequestDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import com.paddle.app.model.User

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {


    @GetMapping("/me")
    fun getMe(authentication: Authentication): ResponseEntity<UserResponseDTO> {
        val firebaseUid = when (val principal = authentication.principal) {
            is User -> principal.firebaseUid
            is Jwt -> principal.subject
            else -> return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = userService.getUserByFirebaseUid(firebaseUid) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.status(HttpStatus.OK).body(user)
    }


    @PostMapping("/onboard")
    fun onboardUser(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody userRequest: UserOnboardRequestDTO):
            ResponseEntity<Void> {
        userService.onboardUser(userRequest = userRequest, firebaseUid =  jwt.subject)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
