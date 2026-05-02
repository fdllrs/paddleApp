package com.paddle.app.controller

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.model.User
import com.paddle.app.service.MatchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/api/matches")
class MatchController(private val matchService: MatchService) {

    @GetMapping("/nearby")
    fun getNearbyMatches(@RequestParam latitude: Double,
                         @RequestParam longitude: Double,
                         @RequestParam radiusMeters: Double,
                         @RequestParam targetDivision: Int,
                         page: Pageable,
                         ): ResponseEntity<Page<MatchResponseDTO>> {

        val nearbyMatches = matchService.getNearbyOpenMatches(latitude, longitude, radiusMeters, targetDivision, page)
        return ResponseEntity.status(HttpStatus.OK).body(nearbyMatches)
    }

    @GetMapping("/{matchId}/players")
    fun getMatchPlayers(@PathVariable matchId: UUID): ResponseEntity<List<UserResponseDTO>> {

        val playersFromMatch = matchService.getPlayersFromMatch(matchId)
        return ResponseEntity.ok(playersFromMatch)
    }

    @GetMapping("/my-matches")
    fun getPlayerMatches(@AuthenticationPrincipal user: User): ResponseEntity<List<MatchResponseDTO>> {

        val matchesForPlayer = matchService.getMatchesForPlayer(user.id!!)
        return ResponseEntity.ok(matchesForPlayer)
    }

    @PostMapping("/{matchId}/join")
    fun joinMatch(
        @PathVariable matchId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        matchService.joinMatch(matchId, user.id!!)
        return ResponseEntity.noContent().build()
    }

    @PostMapping
    fun createMatch(@RequestBody request: MatchCreateRequestDTO): ResponseEntity<MatchResponseDTO> {
        val responseDTO = matchService.createMatch(request)

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO)
    }

    @PostMapping("/{matchId}/leave")
    fun leaveMatch(
        @PathVariable matchId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        matchService.leaveMatch(matchId, user.id!!)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{matchId}/cancel")
    fun cancelMatch(
        @PathVariable matchId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        matchService.cancelMatch(matchId, user.id!!)
        return ResponseEntity.noContent().build()
    }
}