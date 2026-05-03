package com.paddle.app.service

import com.paddle.app.dto.MatchCreateRequestDTO
import com.paddle.app.dto.MatchResponseDTO
import com.paddle.app.dto.UserResponseDTO
import com.paddle.app.dto.toResponseDTO
import com.paddle.app.model.*
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchPlayerRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*


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
        const val MATCH_NOT_OPEN_MESSAGE = "Match not open"
        const val MATCH_FULL_MESSAGE = "Match is full"
        const val MATCH_PLAYED_MESSAGE = "Match is already played"
        const val MATCH_CANCELLED_MESSAGE = "Match is cancelled"
        const val COURT_NOT_FOUND_MESSAGE = "Court not found"
        const val USER_ALREADY_IN_MATCH_MESSAGE = "User is already in this match"
        const val HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE = "Host cannot leave the match"
        const val USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE = "User is not a player in this match"
        const val ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE = "Only the host can cancel the match"
        const val COURT_ALREADY_BOOKED_MESSAGE = "Court is already booked for this time window"


    }

    fun getNearbyOpenMatches(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
        targetDivision: Int,
        page: Pageable
        ): Page<MatchResponseDTO> {

        val userLocationPoint = createPointFromCoordinates(longitude, latitude)

        return matchRepository.findNearbyMatches(MatchStatus.OPEN, userLocationPoint, radiusMeters, targetDivision, page).map { it.toResponseDTO() }
    }

    fun getPlayersFromMatch(matchId: UUID): List<UserResponseDTO> {
        val matchPlayers = matchPlayerRepository.findByMatchId(matchId)

        return matchPlayers.map {it.player.toResponseDTO()}
    }

    fun numberOfPlayersInMatch(matchId: UUID): Int {
        return matchPlayerRepository.countByMatchId(matchId)
    }

    @Transactional(readOnly = true)
    fun getMatchesForPlayer(userId: UUID): List<MatchResponseDTO> {
        val matches = matchPlayerRepository.findByPlayerId(userId)

        return matches.map {it.match.toResponseDTO()}
    }

    @Transactional
    fun createMatch(request: MatchCreateRequestDTO): MatchResponseDTO {
        val court = findCourtById(request.courtId)
        val courtId = requireNotNull(court.id)
        val startDate = request.startDate
        val endDate = startDate.plusMinutes(request.durationMinutes.toLong())

        assertNoOverlappingMatchesInCourt(courtId, startDate, endDate)


        val host = findUserById(request.hostId)
        val newMatch = Match(
            host = host,
            court = court,
            startDate = startDate,
            endDate = endDate,
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

    @Transactional
    fun joinMatch(matchId: UUID, userId: UUID) {
        val match = findMatchByIdForUpdate(matchId)
        val user = findUserById(userId)


        assertMatchIsNotPlayed(match)
        assertMatchIsNotFull(match)
        assertMatchIsOpen(match)


        // 4. Save to the database
        val reservation = MatchPlayer(match = match, player = user)
        matchPlayerRepository.save(reservation)

        val currentPlayerCount = numberOfPlayersInMatch(matchId)
        val newPlayerCount = currentPlayerCount + 1
        if (newPlayerCount >= 4) {
            match.markAsFull()
            matchRepository.save(match)
        }

    }

    @Transactional
    fun leaveMatch(matchId: UUID, userId: UUID) {
        val match = findMatchByIdForUpdate(matchId)
        findUserById(userId)

        if(match.isHost(userId)) throw IllegalArgumentException(HOST_CANNOT_LEAVE_THE_MATCH_MESSAGE)

        val matchPlayer = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, userId) ?:
        throw IllegalArgumentException(USER_IS_NOT_A_PLAYER_IN_THIS_MATCH_MESSAGE)

        matchPlayerRepository.delete(matchPlayer)

        if (match.isFull()) {
            match.markAsOpen()
            matchRepository.save(match)
        }
    }

    @Transactional
    fun cancelMatch(matchId: UUID, userId: UUID) {
        val match = findMatchByIdForUpdate(matchId)
        findUserById(userId)

        assertPlayerIsHostOfMatch(match, userId)

        assertMatchIsNotPlayed(match)
        assertMatchIsNotCancelled(match)

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

    private fun assertPlayerIsHostOfMatch(match: Match, userId: UUID) {
        if (!match.isHost(userId)) throw SecurityException(ONLY_THE_HOST_CAN_CANCEL_THE_MATCH_MESSAGE)
    }

    private fun findUserById(userId: UUID): User {
        return (userRepository.findByIdOrNull(userId)
            ?: throw IllegalArgumentException(USER_NOT_FOUND_MESSAGE))
    }

    private fun findCourtById(courtId: UUID): Court {
        return (courtRepository.findCourtById(courtId)
            ?: throw IllegalArgumentException(COURT_NOT_FOUND_MESSAGE))
    }

    private fun assertMatchIsNotFull(match: Match) {
        if (match.isFull()) throw IllegalArgumentException(MATCH_FULL_MESSAGE)
    }

    private fun assertMatchIsOpen(match: Match) {
        if (!match.isOpen()) throw IllegalArgumentException(MATCH_NOT_OPEN_MESSAGE)
    }

    private fun assertMatchIsNotPlayed(match: Match) {
        if (match.isPlayed()) throw IllegalArgumentException(MATCH_PLAYED_MESSAGE)
    }
    private fun assertMatchIsNotCancelled(match: Match) {
        if (match.isCancelled()) throw IllegalArgumentException(MATCH_CANCELLED_MESSAGE)
    }



    private fun findMatchById(matchId: UUID): Match {
        val match = matchRepository.findByIdOrNull(matchId)
            ?: throw IllegalArgumentException(MATCH_NOT_FOUND_MESSAGE)
        return match
    }

    private fun findMatchByIdForUpdate(matchId: UUID): Match {
        return matchRepository.findByIdForUpdate(matchId)
            ?: throw IllegalArgumentException(MATCH_NOT_FOUND_MESSAGE)
    }

    private fun createPointFromCoordinates(longitude: Double, latitude: Double): Point {
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val coordinate = Coordinate(longitude, latitude)
        val userLocationPoint = geometryFactory.createPoint(coordinate)
        return userLocationPoint
    }

    private fun assertNoOverlappingMatchesInCourt(courtId: UUID, startDate: OffsetDateTime, endDate: OffsetDateTime) {
        if (matchRepository.overlappingMatches(courtId, startDate, endDate)
                .isNotEmpty()
        ) throw IllegalArgumentException(
            COURT_ALREADY_BOOKED_MESSAGE
        )
    }
}