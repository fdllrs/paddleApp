package com.paddle.app.seeder

import com.paddle.app.model.Club
import com.paddle.app.model.Court
import com.paddle.app.model.FloorType
import com.paddle.app.model.Match
import com.paddle.app.model.MatchStatus
import com.paddle.app.model.User
import com.paddle.app.model.WallType
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.CourtRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.OffsetTime


@Component
class DatabaseSeeder(
    private val userRepository: UserRepository,
    private val clubRepository: ClubRepository,
    private val matchRepository: MatchRepository,
    private val courtRepository: CourtRepository,
): CommandLineRunner {

    override fun run(vararg args: String) {
        if (clubRepository.count() > 0) return

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)


        val testUser = User(displayName = "Facundo", division = 7)
        val testClub = Club(
                name = "dummy club Obelisco",
                address = "Obelisco paddle 123",
                location = geometryFactory.createPoint(Coordinate(-58.0, -34.0)),
                openTime = OffsetTime.parse("10:00:00"),
                closeTime = OffsetTime.parse("22:00:00"),
                neighborhood = "VILLA ADELINA"
            )
        val testCourt = Court(
            club = testClub,
            name = "test Court",
            pricePerTurn = 15.toBigDecimal(),
            covered = false,
            wallType = WallType.CEMENT,
            floorType = FloorType.SYNTHETIC_GRASS
        )
        val testMatch = Match(
            host = testUser,
            startDate = OffsetDateTime.now().plusDays(1),
            endDate = OffsetDateTime.now().plusDays(1).plusMinutes(90),
            status = MatchStatus.OPEN,
            court = testCourt,
            targetDivision = 7,
            pricePerPerson = 15.toBigDecimal(),
            durationMinutes = 90
        )

        clubRepository.save(testClub)
        userRepository.save(testUser)
        courtRepository.save(testCourt)
        matchRepository.save(testMatch)
    }
}