package com.paddle.app.repository

import com.paddle.app.model.Club
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClubRepository: JpaRepository<Club, UUID> {




}