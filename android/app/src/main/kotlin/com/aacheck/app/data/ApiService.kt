package com.aacheck.app.data

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // 认证
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    // 旅行
    @POST("api/travels")
    suspend fun createTravel(@Body body: CreateTravelRequest): TravelDto

    @POST("api/travels/join")
    suspend fun joinTravel(@Body body: JoinRequest): TravelDto

    @GET("api/travels")
    suspend fun myTravels(): List<TravelDto>

    @GET("api/travels/{id}")
    suspend fun travelDetail(@Path("id") id: Long): TravelDto

    @PATCH("api/travels/{id}")
    suspend fun updateTravel(@Path("id") id: Long, @Body body: Map<String, String>): TravelDto

    @POST("api/travels/{id}/end")
    suspend fun endTravel(@Path("id") id: Long): TravelDto

    @POST("api/travels/{id}/invite-code/regenerate")
    suspend fun regenerateInviteCode(@Path("id") id: Long): TravelDto

    @DELETE("api/travels/{id}")
    suspend fun deleteTravel(@Path("id") id: Long): Map<String, String>

    // 支出
    @GET("api/travels/{travelId}/expenses")
    suspend fun expenses(@Path("travelId") travelId: Long): List<ExpenseView>

    @GET("api/travels/{travelId}/approvals")
    suspend fun pendingApprovals(@Path("travelId") travelId: Long): List<PendingApproval>

    @POST("api/travels/{travelId}/expenses")
    suspend fun createExpense(@Path("travelId") travelId: Long, @Body body: ExpenseUpsertRequest): ExpenseView

    @GET("api/expenses/{id}")
    suspend fun expense(@Path("id") id: Long): ExpenseView

    @PATCH("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") id: Long, @Body body: ExpenseUpsertRequest): ExpenseView

    @POST("api/expenses/{id}/approve")
    suspend fun approve(@Path("id") id: Long): Map<String, String>

    @POST("api/expenses/{id}/disagree")
    suspend fun disagree(@Path("id") id: Long): Map<String, String>

    @POST("api/expenses/{id}/withdraw")
    suspend fun withdraw(@Path("id") id: Long): Map<String, String>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Long): Map<String, String>

    // 结算
    @GET("api/travels/{travelId}/settlement")
    suspend fun settlement(@Path("travelId") travelId: Long): SettlementDto

    // 图片上传
    @Multipart
    @POST("api/images")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Map<String, String>
}
