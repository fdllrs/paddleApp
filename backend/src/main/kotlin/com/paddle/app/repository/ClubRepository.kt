package com.paddle.app.repository

import com.paddle.app.model.Club
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClubRepository : JpaRepository<Club, UUID> {

}