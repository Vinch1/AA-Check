package com.aacheck.app.core

import android.content.Context
import com.aacheck.app.data.UserDto
import com.google.gson.Gson

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("aacheck", Context.MODE_PRIVATE)
    private val gson = Gson()

    val token: String?
        get() = prefs.getString("token", null)

    val user: UserDto?
        get() = prefs.getString("user", null)?.let { gson.fromJson(it, UserDto::class.java) }

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
        set(v) = prefs.edit().putString("theme_mode", v.name).apply()

    fun isLoggedIn() = !token.isNullOrBlank()

    fun saveAuth(token: String, user: UserDto): Boolean =
        prefs.edit()
            .putString("token", token)
            .putString("user", gson.toJson(user))
            .commit()

    fun clear(): Boolean =
        prefs.edit()
            .remove("token")
            .remove("user")
            .commit()
}
