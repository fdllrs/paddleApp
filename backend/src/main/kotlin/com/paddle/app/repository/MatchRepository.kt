package com.paddle.app.repository

import com.paddle.app.model.Match
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MatchRepository: JpaRepository<Match, UUID> {

    @Query("SELECT m FROM Match m JOIN m.club c WHERE m.status = :status AND distance(c.location, :userLocation) <= :radius AND m.targetDivision = :targetDivision")
    fun findNearbyMatches(@Param("status") status: String,
                          @Param("userLocation") userLocation: Point,
                          @Param("radius") radius: Double,
                          @Param("targetDivision") targetDivision: Int
    ): List<Match>



}