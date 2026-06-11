package com.paddle.app.exception

import com.paddle.app.service.MatchService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice



@RestControllerAdvice
class GlobalExceptionHandler {


    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDuplicateJoin(ex: DataIntegrityViolationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            mapOf("error" to
                MatchService.USER_ALREADY_IN_MATCH_MESSAGE))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotFound(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidOnboard(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))
    }

    @ExceptionHandler(AllCourtsBookedException::class)
    fun handleAllCourtsBooked(ex: AllCourtsBookedException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))
    }

    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(ex: SecurityException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to ex.message))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))
    }
}