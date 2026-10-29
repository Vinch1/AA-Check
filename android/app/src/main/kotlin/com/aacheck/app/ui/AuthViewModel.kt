package com.aacheck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aacheck.app.data.Repository
import com.aacheck.app.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: Repository) : ViewModel() {
    data class State(
        val busy: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            _state.value = State(error = "请输入手机号和密码"); return
        }
        _state.value = State(busy = true)
        viewModelScope.launch {
            runCatching { repo.login(phone.trim(), password) }
                .onSuccess { _state.value = State(success = true) }
                .onFailure { _state.value = State(error = friendly(it)) }
        }
    }

    fun register(phone: String, password: String, nickname: String) {
        if (phone.isBlank() || password.length < 6) {
            _state.value = State(error = "手机号不能为空，密码至少 6 位"); return
        }
        _state.value = State(busy = true)
        viewModelScope.launch {
            runCatching { repo.register(phone.trim(), password, nickname.trim().ifEmpty { null }) }
                .onSuccess { _state.value = State(success = true) }
                .onFailure { _state.value = State(error = friendly(it)) }
        }
    }

    private fun friendly(e: Throwable): String = e.toUserMessage()
    fun clearError() { _state.value = _state.value.copy(error = null) }
}
