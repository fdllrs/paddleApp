package com.paddle.app.repository

import com.paddle.app.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository: JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findUserById(@Param("id") id: UUID): User?
}