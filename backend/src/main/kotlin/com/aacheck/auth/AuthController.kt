package com.aacheck.auth

import com.aacheck.user.User
import com.aacheck.user.UserRepository
import com.aacheck.common.badRequest
import com.aacheck.security.JwtService
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

data class RegisterRequest(
    @field:NotBlank val phone: String? = null,
    @field:NotBlank val password: String? = null,
    val nickname: String? = null,
)

data class LoginRequest(
    @field:NotBlank val phone: String? = null,
    @field:NotBlank val password: String? = null,
)

data class UserDto(val id: Long, val phone: String, val nickname: String, val avatarUrl: String?)

data class AuthResponse(val token: String, val user: UserDto)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @PostMapping("/register")
    fun register(@RequestBody req: RegisterRequest): AuthResponse {
        val phone = req.phone!!.trim()
        val password = req.password!!
        if (!Regex("^1\\d{10}$").matches(phone)) throw badRequest("手机号格式不正确")
        if (password.length < 6) throw badRequest("密码至少 6 位")
        if (userRepository.existsByPhone(phone)) throw badRequest("该手机号已注册")
        val user = userRepository.save(
            User(
                phone = phone,
                passwordHash = passwordEncoder.encode(password),
                nickname = req.nickname?.trim().takeUnless { it.isNullOrEmpty() } ?: "用户$phone".take(12),
            )
        )
        return AuthResponse(jwtService.generate(user.id), user.toDto())
    }

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): AuthResponse {
        val user = userRepository.findByPhone(req.phone!!.trim()) ?: throw badRequest("手机号或密码错误")
        if (!passwordEncoder.matches(req.password!!, user.passwordHash)) throw badRequest("手机号或密码错误")
        return AuthResponse(jwtService.generate(user.id), user.toDto())
    }
}

fun User.toDto() = UserDto(id, phone, nickname, avatarUrl)
