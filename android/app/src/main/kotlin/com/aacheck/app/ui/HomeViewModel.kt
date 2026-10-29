package com.aacheck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aacheck.app.data.Repository
import com.aacheck.app.data.TravelDto
import com.aacheck.app.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: Repository) : ViewModel() {
    data class State(
        val travels: List<TravelDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repo.myTravels() }
                .onSuccess { _state.value = State(travels = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = friendly(it)) }
        }
    }

    fun create(name: String, note: String?, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.createTravel(name, note) }
                .onSuccess { onDone(it.id) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun join(code: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.joinTravel(code) }
                .onSuccess { onDone(it.id) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun logout() { repo.logout() }

    private fun friendly(e: Throwable): String = e.toUserMessage()
    fun clearError() { _state.value = _state.value.copy(error = null) }
}
