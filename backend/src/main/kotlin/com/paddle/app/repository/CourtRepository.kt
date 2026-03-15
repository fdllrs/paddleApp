package com.paddle.app.repository

import com.paddle.app.model.Court
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID


interface CourtRepository: JpaRepository<Court, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findCourtById(id: UUID): Court?
}