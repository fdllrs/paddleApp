package com.paddle.app.controller

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.service.MatchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/matches")
class MatchController(private val matchService: MatchService) {

    @GetMapping("/nearby")
    fun getNearbyMatches(@RequestParam latitude: Double,
                         @RequestParam longitude: Double,
                         @RequestParam radiusMeters: Double,
                         @RequestParam targetDivision: Int
                         ): List<MatchResponseDTO> {

        return matchService.getNearbyOpenMatches(latitude, longitude, radiusMeters, targetDivision)
    }

    @GetMapping("/{matchId}/players")
    fun getMatchPlayers(@PathVariable matchId: UUID): List<UserResponseDTO> {

        return matchService.getPlayersFromMatch(matchId)
    }

    @GetMapping("/my-matches")
    fun getPlayerMatches(@RequestParam userId: UUID): List<MatchResponseDTO> {
        return matchService.getMatchesForPlayer(userId)
    }

    @PostMapping("/{matchId}/join")
    fun joinMatch(
        @PathVariable matchId: UUID,
        @RequestParam userId: UUID
    ) {
        return matchService.joinMatch(matchId, userId)
    }

    @PostMapping
    fun createMatch(@RequestBody request: MatchCreateRequestDTO): ResponseEntity<MatchResponseDTO> {
        val responseDTO = matchService.createMatch(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO)

    }

}