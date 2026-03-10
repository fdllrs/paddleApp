package com.paddle.app.repository

import com.paddle.app.model.MatchPlayer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MatchPlayerRepository : JpaRepository<MatchPlayer, UUID> {


    fun findByMatchId(matchId: UUID): List<MatchPlayer>

    fun findByPlayerId(playerId: UUID): List<MatchPlayer>

    fun findByMatchIdAndPlayerId(matchId: UUID, playerId: UUID): MatchPlayer?
}