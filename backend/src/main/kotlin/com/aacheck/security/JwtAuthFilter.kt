package com.aacheck.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val userId = jwtService.parseUserId(header.removePrefix("Bearer ").trim())
            if (userId != null) {
                val auth = UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("USER")))
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }
}

/** 从 SecurityContext 取当前用户 id，未登录返回 null */
fun currentUser(): Long? =
    SecurityContextHolder.getContext()?.authentication?.principal as? Long
