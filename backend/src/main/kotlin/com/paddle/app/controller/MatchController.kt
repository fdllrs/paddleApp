package com.paddle.app.controller

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.service.MatchService
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
                         @RequestParam radiusMeters: Double): List<MatchResponseDTO> {

        return matchService.getNearbyOpenMatches(latitude, longitude, radiusMeters)
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
    ): String {
        matchService.joinMatch(matchId, userId)
        return "successfully joined the match"
    }

    @PostMapping
    fun createMatch(@RequestBody request: MatchCreateRequestDTO): MatchResponseDTO {
        return matchService.createMatch(request)

    }

}