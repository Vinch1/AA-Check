package com.aacheck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aacheck.app.data.Repository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class BaseViewModel(protected val repo: Repository) : ViewModel() {

    data class UiState(val loading: Boolean = false, val error: String? = null)

    protected val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val handler = CoroutineExceptionHandler { _, e ->
        _ui.value = _ui.value.copy(loading = false, error = friendlyMessage(e))
    }

    fun friendlyMessage(e: Throwable): String {
        val msg = e.message ?: return "网络请求失败，请稍后重试"
        val http = e as? retrofit2.HttpException
        if (http != null) {
            val body = http.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                return runCatching {
                    com.google.gson.Gson().fromJson(body, com.aacheck.app.data.ErrorBody::class.java)?.message
                }.getOrNull() ?: msg
            }
            if (http.code() == 401) return "登录已过期，请重新登录"
        }
        return msg
    }

    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch(handler) {
            _ui.value = _ui.value.copy(loading = true, error = null)
            block()
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
}
