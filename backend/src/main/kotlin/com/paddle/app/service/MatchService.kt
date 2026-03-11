package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.Club
import com.paddle.app.model.Match
import com.paddle.app.model.MatchPlayer
import com.paddle.app.model.User
import com.paddle.app.repository.ClubRepository
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
import java.time.Clock
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID



@Service
class MatchService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val matchPlayerRepository: MatchPlayerRepository,
    private val clock: Clock
    ) {

    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_CLOSED = "CLOSED"
        const val STATUS_CANCELLED = "CANCELLED"
        const val MATCH_NOT_FOUND_MESSAGE = "Match not found"
        const val USER_NOT_FOUND_MESSAGE = "User not found"
        const val MATCH_NOT_OPEN_MESSAGE = "Match is not open"
        const val CLUB_NOT_FOUND_MESSAGE = "Club not found"
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

        return matchRepository.findNearbyMatches(STATUS_OPEN, userLocationPoint, radiusMeters, targetDivision).map { it.toResponseDTO() }
    }

    fun getPlayersFromMatch(matchId: UUID): List<UserResponseDTO> {
        val matchPlayers = matchPlayerRepository.findByMatchId(matchId)

        return matchPlayers.map {it.player.toResponseDTO()}

    }

    @Transactional(readOnly = true)
    fun getMatchesForPlayer(userId: UUID): List<MatchResponseDTO> {
        val matches = matchPlayerRepository.findByPlayerId(userId)

        return matches.map {it.match.toResponseDTO()}
    }

    fun createMatch(request: MatchCreateRequestDTO): MatchResponseDTO {
        val host = findUserById(request.hostId)
        val club = findClubById(request.clubId)

        val newMatch = Match(
            host = host,
            club = club,
            matchDate = request.matchDate,
            durationMinutes = request.durationMinutes,
            pricePerPerson = request.pricePerPerson,
            status = STATUS_OPEN,
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

    }

    fun leaveMatch(matchId: UUID, userId: UUID) {
        val match = findMatchById(matchId)
        findUserById(userId)

        if(match.host.id == userId) throw IllegalArgumentException(HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE)

        val matchPlayer = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, userId) ?:
        throw IllegalArgumentException(USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE)

        matchPlayerRepository.delete(matchPlayer)
    }

    fun cancelMatch(matchId: UUID, userId: UUID) {
        val match = findMatchById(matchId)
        findUserById(userId)

        if (match.host.id != userId) throw SecurityException(ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE)

        assertMatchIsOpen(match)

        match.status = STATUS_CANCELLED
        matchRepository.save(match)

        // TODO: Fetch matchPlayerRepository.findByMatchId(matchId) and send push notifications to other users
    }





    private fun calculateTimeUntilMatch(match: Match): Duration {
        val now = OffsetDateTime.now(clock)
        val timeUntilMatch = Duration.between(now, match.matchDate)
        return timeUntilMatch
    }

    private fun findUserById(userId: UUID): User {
        return (userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException(USER_NOT_FOUND_MESSAGE))
    }

    private fun findClubById(clubId: UUID): Club {
        return (clubRepository.findByIdOrNull(clubId)
            ?: throw IllegalArgumentException(CLUB_NOT_FOUND_MESSAGE))
    }

    private fun assertMatchIsOpen(match: Match) {
        if (match.status != STATUS_OPEN) throw IllegalArgumentException(MATCH_NOT_OPEN_MESSAGE)
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