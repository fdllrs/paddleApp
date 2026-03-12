package com.paddle.app.repository

import com.paddle.app.model.Club
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ClubRepository : JpaRepository<Club, UUID> {

    @Query("""
        SELECT c 
        FROM Club c 
        WHERE ST_DWithin(c.location, :location1, :radius1) = true 
        AND ST_DWithin(c.location, :location2, :radius2) = true
    """)
    fun findClubsInIntersection(
        @Param("location1") location1: Point,
        @Param("radius1") radius1: Double,
        @Param("location2") location2: Point,
        @Param("radius2") radius2: Double
    ): List<Club>
}