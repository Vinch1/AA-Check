package com.aacheck.app.data

import com.aacheck.app.core.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class Repository(
    private val api: ApiService,
    val tokenStore: TokenStore,
) {

    // ---------- 认证 ----------
    suspend fun login(phone: String, password: String): AuthResponse {
        val r = api.login(LoginRequest(phone, password))
        tokenStore.token = r.token
        tokenStore.user = r.user
        return r
    }

    suspend fun register(phone: String, password: String, nickname: String?): AuthResponse {
        val r = api.register(RegisterRequest(phone, password, nickname))
        tokenStore.token = r.token
        tokenStore.user = r.user
        return r
    }

    fun logout() = tokenStore.clear()

    // ---------- 旅行 ----------
    suspend fun myTravels(): List<TravelDto> = api.myTravels()
    suspend fun travelDetail(id: Long): TravelDto = api.travelDetail(id)
    suspend fun createTravel(name: String, note: String?): TravelDto = api.createTravel(CreateTravelRequest(name, note))
    suspend fun joinTravel(code: String): TravelDto = api.joinTravel(JoinRequest(code.trim().uppercase()))
    suspend fun endTravel(id: Long): TravelDto = api.endTravel(id)
    suspend fun regenerateInviteCode(id: Long): TravelDto = api.regenerateInviteCode(id)
    suspend fun deleteTravel(id: Long): Map<String, String> = api.deleteTravel(id)
    suspend fun updateTravel(id: Long, name: String): TravelDto = api.updateTravel(id, mapOf("name" to name))

    // ---------- 支出 ----------
    suspend fun expenses(travelId: Long): List<ExpenseView> = api.expenses(travelId)
    suspend fun expense(id: Long): ExpenseView = api.expense(id)
    suspend fun pendingApprovals(travelId: Long): List<PendingApproval> = api.pendingApprovals(travelId)
    suspend fun createExpense(travelId: Long, req: ExpenseUpsertRequest): ExpenseView = api.createExpense(travelId, req)
    suspend fun updateExpense(id: Long, req: ExpenseUpsertRequest): ExpenseView = api.updateExpense(id, req)
    suspend fun approve(id: Long) = api.approve(id)
    suspend fun disagree(id: Long) = api.disagree(id)
    suspend fun withdraw(id: Long) = api.withdraw(id)
    suspend fun deleteExpense(id: Long) = api.deleteExpense(id)

    // ---------- 结算 ----------
    suspend fun settlement(travelId: Long): SettlementDto = api.settlement(travelId)

    /** ---------- 图片上传 ---------- */
    suspend fun uploadImage(file: File): String = withContext(Dispatchers.IO) {
        val reqBody = file.readBytes().toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, reqBody)
        api.uploadImage(part)["url"] ?: error("服务器未返回图片地址")
    }
}
