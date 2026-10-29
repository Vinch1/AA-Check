package com.aacheck.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aacheck.app.data.ExpenseUpsertRequest
import com.aacheck.app.data.ExpenseView
import com.aacheck.app.data.PendingApproval
import com.aacheck.app.data.Repository
import com.aacheck.app.data.SettlementDto
import com.aacheck.app.data.TravelDto
import com.aacheck.app.data.WsClient
import com.aacheck.app.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TravelViewModel(
    val repo: Repository,
    private val wsClient: WsClient,
) : ViewModel() {

    data class State(
        val travel: TravelDto? = null,
        val expenses: List<ExpenseView> = emptyList(),
        val approvals: List<PendingApproval> = emptyList(),
        val settlement: SettlementDto? = null,
        val loading: Boolean = false,
        val error: String? = null,
        // 过滤器：null = 全部
        val filterMemberId: Long? = null,
        val filterCategory: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    var currentUserId: Long? = null
        private set

    fun load(travelId: Long) {
        currentUserId = repo.tokenStore.user?.id
        _state.value = State(loading = true)
        viewModelScope.launch {
            runCatching { repo.travelDetail(travelId) }
                .onSuccess { t -> _state.value = _state.value.copy(travel = t, loading = false) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = friendly(e)) }
            refreshExpenses(travelId)
            refreshApprovals(travelId)
            refreshSettlement(travelId)
            wsClient.connect()
            wsClient.subscribe(travelId)
        }
    }

    fun onWsEvent(travelId: Long) {
        viewModelScope.launch {
            refreshExpenses(travelId)
            refreshApprovals(travelId)
            refreshSettlement(travelId)
        }
    }

    fun refreshExpenses(travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.expenses(travelId) }
                .onSuccess { _state.value = _state.value.copy(expenses = it) }
        }
    }

    private fun refreshApprovals(travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.pendingApprovals(travelId) }
                .onSuccess { _state.value = _state.value.copy(approvals = it) }
        }
    }

    fun refreshSettlement(travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.settlement(travelId) }
                .onSuccess { _state.value = _state.value.copy(settlement = it) }
        }
    }

    fun setFilter(memberId: Long?, category: String?) {
        _state.value = _state.value.copy(filterMemberId = memberId, filterCategory = category)
    }

    // ---------- 操作 ----------

    fun createExpense(travelId: Long, req: ExpenseUpsertRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.createExpense(travelId, req) }
                .onSuccess { onDone(); refreshExpenses(travelId); refreshSettlement(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun updateExpense(expenseId: Long, travelId: Long, req: ExpenseUpsertRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.updateExpense(expenseId, req) }
                .onSuccess { onDone(); refreshExpenses(travelId); refreshSettlement(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun deleteExpense(expenseId: Long, travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteExpense(expenseId) }
                .onSuccess { refreshExpenses(travelId); refreshSettlement(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun approve(expenseId: Long, travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.approve(expenseId) }
                .onSuccess { refreshApprovals(travelId); refreshExpenses(travelId); refreshSettlement(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun disagree(expenseId: Long, travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.disagree(expenseId) }
                .onSuccess { refreshApprovals(travelId); refreshExpenses(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun withdraw(expenseId: Long, travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.withdraw(expenseId) }
                .onSuccess { refreshApprovals(travelId); refreshExpenses(travelId); refreshSettlement(travelId) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun endTravel(travelId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.endTravel(travelId) }
                .onSuccess { onDone(); _state.value = _state.value.copy(travel = it) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun renameTravel(travelId: Long, name: String) {
        viewModelScope.launch {
            runCatching { repo.updateTravel(travelId, name) }
                .onSuccess { _state.value = _state.value.copy(travel = it) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun regenerateCode(travelId: Long) {
        viewModelScope.launch {
            runCatching { repo.regenerateInviteCode(travelId) }
                .onSuccess { _state.value = _state.value.copy(travel = it) }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    fun deleteTravel(travelId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repo.deleteTravel(travelId) }
                .onSuccess { onDone() }
                .onFailure { _state.value = _state.value.copy(error = friendly(it)) }
        }
    }

    private fun friendly(e: Throwable): String = e.toUserMessage()
    fun clearError() { _state.value = _state.value.copy(error = null) }
}
