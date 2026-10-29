package com.aacheck.user

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 20)
    val phone: String = "",

    @Column(nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false, length = 32)
    var nickname: String = "",

    var avatarUrl: String? = null,

    val createdAt: java.time.Instant = java.time.Instant.now(),
)

interface UserRepository : org.springframework.data.jpa.repository.JpaRepository<User, Long> {
    fun findByPhone(phone: String): User?
    fun existsByPhone(phone: String): Boolean
}
