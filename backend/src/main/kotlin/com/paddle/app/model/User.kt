package com.paddle.app.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "firebase_uid", nullable = false, length = 28, unique = true)
    val firebaseUid: String,

    @Column(name = "display_name", nullable = false, length = 50)
    var displayName: String,

    @Column(name = "division", nullable = false)
    var division: Int,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null
)