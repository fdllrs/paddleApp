package com.paddle.app.seeder

import com.paddle.app.model.Club
import com.paddle.app.model.Match
import com.paddle.app.model.User
import com.paddle.app.repository.ClubRepository
import com.paddle.app.repository.MatchRepository
import com.paddle.app.repository.UserRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID


@Component
class DatabaseSeeder(private val userRepository: UserRepository, private val clubRepository: ClubRepository, private val matchRepository: MatchRepository): CommandLineRunner {

    override fun run(vararg args: String) {
        if (clubRepository.count() > 0) return


        val dummyUser = User(displayName = "Facundo", division = 7)
        userRepository.save(dummyUser)

        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        val coordinate = Coordinate(-58.39, -34.60)
        val dummyClubCoordinates = geometryFactory.createPoint(coordinate)
        val dummyClub = Club(name = "dummy club Obelisco", address = "Obelisco paddle", location = dummyClubCoordinates)
        clubRepository.save(dummyClub)

        val dummyMatch = Match(
            host = dummyUser,
            matchDate = OffsetDateTime.now().plusDays(1),
            status = "OPEN",
            club = dummyClub,
            targetDivision = 7,
            pricePerPerson = 15F)
        matchRepository.save(dummyMatch)


    }
}