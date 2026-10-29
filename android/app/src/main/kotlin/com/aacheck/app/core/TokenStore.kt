package com.aacheck.app.core

import android.content.Context
import com.aacheck.app.data.UserDto
import com.google.gson.Gson

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("aacheck", Context.MODE_PRIVATE)
    private val gson = Gson()

    var token: String?
        get() = prefs.getString("token", null)
        set(v) = prefs.edit().putString("token", v).apply()

    var user: UserDto?
        get() = prefs.getString("user", null)?.let { gson.fromJson(it, UserDto::class.java) }
        set(v) = prefs.edit().putString("user", v?.let { gson.toJson(it) }).apply()

    fun isLoggedIn() = !token.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
