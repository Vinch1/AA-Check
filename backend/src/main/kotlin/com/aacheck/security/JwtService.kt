package com.aacheck.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expire-days}") private val expireDays: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generate(userId: Long): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expireDays * 24 * 3600 * 1000))
            .signWith(key)
            .compact()

    fun parseUserId(token: String): Long? = try {
        val claims: Claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        claims.subject?.toLongOrNull()
    } catch (_: Exception) {
        null
    }
}
