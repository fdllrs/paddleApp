package com.paddle.app.repository

import com.paddle.app.model.Court
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID


interface CourtRepository: JpaRepository<Court, UUID> {

}