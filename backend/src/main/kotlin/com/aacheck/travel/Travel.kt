package com.aacheck.travel

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "travels")
class Travel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 64)
    var name: String = "",

    @Column(length = 256)
    var note: String? = null,

    @Column(nullable = false, unique = true, length = 8)
    var inviteCode: String = "",

    /** ACTIVE / ENDED */
    @Column(nullable = false, length = 16)
    var status: String = "ACTIVE",

    @Column(nullable = false)
    val creatorId: Long = 0,

    val createdAt: Instant = Instant.now(),

    var endedAt: Instant? = null,
)

@Entity
@Table(
    name = "travel_members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["travelId", "userId"])],
)
class TravelMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val travelId: Long = 0,

    @Column(nullable = false)
    val userId: Long = 0,

    val joinedAt: Instant = Instant.now(),
)

interface TravelRepository : org.springframework.data.jpa.repository.JpaRepository<Travel, Long> {
    fun findByInviteCode(code: String): Travel?
    fun existsByInviteCode(code: String): Boolean
}

interface TravelMemberRepository : org.springframework.data.jpa.repository.JpaRepository<TravelMember, Long> {
    fun existsByTravelIdAndUserId(travelId: Long, userId: Long): Boolean
    fun findByTravelId(travelId: Long): List<TravelMember>
    fun findByUserId(userId: Long): List<TravelMember>
    fun findByTravelIdAndUserId(travelId: Long, userId: Long): TravelMember?
    fun deleteByTravelId(travelId: Long)
}
