package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.Match
import com.paddle.app.model.MatchPlayer
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.MatchPlayerRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class MatchService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val matchPlayerRepository: MatchPlayerRepository) {

    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_CLOSED = "CLOSED"
        const val MATCH_NOT_FOUND_MESSAGE = "Match not found"
        const val USER_NOT_FOUND_MESSAGE = "User not found"
        const val MATCH_NOT_OPEN_MESSAGE = "Match is not open"
        const val HOST_USER_NOT_FOUND_MESSAGE = "Host user not found"
        const val CLUB_NOT_FOUND_MESSAGE = "Club not found"
    }

    fun getNearbyOpenMatches(latitude: Double, longitude: Double, radiusMeters: Double): List<MatchResponseDTO> {

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val coordinate = Coordinate(longitude, latitude)
        val userLocationPoint = geometryFactory.createPoint(coordinate)

        return matchRepository.findNearbyMatches(STATUS_OPEN, userLocationPoint, radiusMeters).map { it.toResponseDTO() }
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
        val host = userRepository.findByIdOrNull(request.hostId)
            ?: throw IllegalArgumentException(HOST_USER_NOT_FOUND_MESSAGE)

        val club = clubRepository.findByIdOrNull(request.clubId)
            ?: throw IllegalArgumentException(CLUB_NOT_FOUND_MESSAGE)

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
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw IllegalArgumentException(MATCH_NOT_FOUND_MESSAGE)

        val user = userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException(USER_NOT_FOUND_MESSAGE)

        if (match.status != STATUS_OPEN) throw IllegalArgumentException(MATCH_NOT_OPEN_MESSAGE)

        val reservation = MatchPlayer(match = match, player = user)

        // 4. Save to the database
        matchPlayerRepository.save(reservation)

    }



}