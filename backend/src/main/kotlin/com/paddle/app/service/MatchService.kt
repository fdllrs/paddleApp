package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.Court
import com.paddle.app.model.Match
import com.paddle.app.model.MatchPlayer
import com.paddle.app.model.MatchStatus
import com.paddle.app.model.User
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchPlayerRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID



@Service
class MatchService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val courtRepository: CourtRepository,
    private val matchPlayerRepository: MatchPlayerRepository,
    ) {

    companion object {
        const val MATCH_NOT_FOUND_MESSAGE = "Match not found"
        const val USER_NOT_FOUND_MESSAGE = "User not found"
        const val MATCH_FULL_MESSAGE = "Match is full"
        const val COURT_NOT_FOUND_MESSAGE = "Court not found"
        const val USER_ALREADY_IN_MATCH_MESSAGE = "User is already in this match"
        const val HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE = "Host cannot leave the match"
        const val USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE = "User is not a player in this match"
        const val ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE = "Only the host can cancel the match"


    }

    fun getNearbyOpenMatches(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        targetDivision: Int): List<MatchResponseDTO> {

        val userLocationPoint = createPointFromCoordinates(longitude, latitude)

        return matchRepository.findNearbyMatches(MatchStatus.OPEN, userLocationPoint, radiusMeters, targetDivision).map { it.toResponseDTO() }
    }

    fun getPlayersFromMatch(matchId: UUID): List<UserResponseDTO> {
        val matchPlayers = matchPlayerRepository.findByMatchId(matchId)

        return matchPlayers.map {it.player.toResponseDTO()}
    }

    fun numberOfPlayersInMatch(matchId: UUID): Int {
        val matchPlayers = matchPlayerRepository.findByMatchId(matchId)
        return matchPlayers.count()

    }

    @Transactional(readOnly = true)
    fun getMatchesForPlayer(userId: UUID): List<MatchResponseDTO> {
        val matches = matchPlayerRepository.findByPlayerId(userId)

        return matches.map {it.match.toResponseDTO()}
    }

    fun createMatch(request: MatchCreateRequestDTO): MatchResponseDTO {
        val host = findUserById(request.hostId)
        val court = findCourtById(request.courtId)

        val newMatch = Match(
            host = host,
            court = court,
            matchDate = request.matchDate,
            durationMinutes = request.durationMinutes,
            pricePerPerson = request.pricePerPerson,
            status = MatchStatus.OPEN,
            targetDivision = request.targetDivision
        )

        val savedMatch = matchRepository.save(newMatch)
        val matchId = requireNotNull(savedMatch.id)

        this.joinMatch(matchId, request.hostId)

        return savedMatch.toResponseDTO()
    }

    fun joinMatch(matchId: UUID, userId: UUID) {
        val match = findMatchById(matchId)
        val user = findUserById(userId)

        assertMatchIsOpen(match)

        val reservation = MatchPlayer(match = match, player = user)

        // 4. Save to the database
        matchPlayerRepository.save(reservation)

        if (numberOfPlayersInMatch(matchId) == 4) {
            match.markAsFull()
            matchRepository.save(match)
        }
    }

    fun leaveMatch(matchId: UUID, userId: UUID) {
        val match = findMatchById(matchId)
        findUserById(userId)

        if(match.isHost(userId)) throw IllegalArgumentException(HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE)

        val matchPlayer = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, userId) ?:
        throw IllegalArgumentException(USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE)

        matchPlayerRepository.delete(matchPlayer)
    }

    fun cancelMatch(matchId: UUID, userId: UUID) {
        val match = findMatchById(matchId)
        findUserById(userId)

        if (!match.isHost(userId)) throw SecurityException(ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE)

        assertMatchIsOpen(match)
        match.markAsCancelled()

        matchRepository.save(match)

        // TODO: Fetch matchPlayerRepository.findByMatchId(matchId) and send push notifications to other users
    }

    fun filterDuoCompatibleMatches(matches: List<MatchResponseDTO>): List<MatchResponseDTO> {
        return matches.filter { match ->
            val matchId = requireNotNull(match.id)
            numberOfPlayersInMatch(matchId) >= 3
        }
    }
    fun isPlayerInMatch(matchId: UUID, playerId: UUID): Boolean {
        return matchPlayerRepository.findByMatchId(matchId).any { it.player.id == playerId }
    }

    private fun findUserById(userId: UUID): User {
        return (userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException(USER_NOT_FOUND_MESSAGE))
    }

    private fun findCourtById(courtId: UUID): Court {
        return (courtRepository.findByIdOrNull(courtId)
            ?: throw IllegalArgumentException(COURT_NOT_FOUND_MESSAGE))
    }

    private fun assertMatchIsOpen(match: Match) {
        if (!match.isOpen()) throw IllegalArgumentException(MATCH_FULL_MESSAGE)
    }

    private fun findMatchById(matchId: UUID): Match {
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw IllegalArgumentException(MATCH_NOT_FOUND_MESSAGE)
        return match
    }

    private fun createPointFromCoordinates(longitude: Double, latitude: Double): Point {
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val coordinate = Coordinate(longitude, latitude)
        val userLocationPoint = geometryFactory.createPoint(coordinate)
        return userLocationPoint
    }

}