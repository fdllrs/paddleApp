package com.paddle.app.repository

import com.paddle.app.model.Match
import com.paddle.app.model.MatchStatus
import jakarta.persistence.LockModeType
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface MatchRepository: JpaRepository<Match, UUID> {

    @Query("SELECT m FROM Match m JOIN m.court ct JOIN ct.club c WHERE m.status = :status AND distance(c.location, :userLocation) <= :radius AND m.targetDivision = :targetDivision")
    fun findNearbyMatches(@Param("status") status: MatchStatus,
                          @Param("userLocation") userLocation: Point,
                          @Param("radius") radius: Double,
                          @Param("targetDivision") targetDivision: Int
    ): List<Match>



    @Query("SELECT m FROM Match m WHERE m.court.id = :courtId AND m.startDate < :newEndTime AND m.endDate > :newStartTime")
    fun overlappingMatches(
        @Param("courtId") courtId: UUID,
        @Param("newStartTime") newStartTime: OffsetDateTime,
        @Param("newEndTime") newEndTime: OffsetDateTime
    ): List<Match>


}