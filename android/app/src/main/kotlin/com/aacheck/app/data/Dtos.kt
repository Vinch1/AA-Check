package com.aacheck.app.data

// ---------- 认证 ----------
data class UserDto(val id: Long, val phone: String, val nickname: String, val avatarUrl: String?)
data class AuthResponse(val token: String, val user: UserDto)
data class RegisterRequest(val phone: String, val password: String, val nickname: String? = null)
data class LoginRequest(val phone: String, val password: String)

// ---------- 旅行 ----------
data class MemberBrief(val userId: Long, val nickname: String, val avatarUrl: String?, val isCreator: Boolean)
data class TravelDto(
    val id: Long,
    val name: String,
    val note: String?,
    val status: String,
    val inviteCode: String,
    val amCreator: Boolean,
    val createdAt: String,
    val endedAt: String?,
    val members: List<MemberBrief>,
)
data class CreateTravelRequest(val name: String, val note: String? = null)
data class JoinRequest(val code: String)

// ---------- 支出 ----------
data class ShareLine(val userId: Long, val shareCents: Long)
data class SnapshotShare(val userId: Long, val shareCents: Long)
data class AccountSnapshot(
    val amountCents: Long,
    val payerId: Long,
    val category: String,
    val shares: List<SnapshotShare>,
)
data class OpenEditView(
    val editId: Long,
    val editorId: Long,
    val status: String,
    val before: AccountSnapshot,
    val after: AccountSnapshot,
)
data class ExpenseView(
    val id: Long,
    val travelId: Long,
    val authorId: Long,
    val payerId: Long,
    val amountCents: Long,
    val category: String,
    val note: String,
    val createdAt: String,
    val updatedAt: String,
    val shares: List<ShareLine>,
    val images: List<String>,
    val openEdit: OpenEditView?,
)
data class ExpenseUpsertRequest(
    val payerId: Long? = null,
    val amountCents: Long? = null,
    val category: String? = null,
    val note: String? = null,
    val participantIds: List<Long>? = null,
    val imageUrls: List<String>? = null,
)
data class PendingApproval(
    val editId: Long,
    val expenseId: Long,
    val editorId: Long,
    val before: AccountSnapshot,
    val after: AccountSnapshot,
    val expense: ExpenseView,
)

// ---------- 结算 ----------
data class MemberBalance(val userId: Long, val nickname: String, val avatarUrl: String?, val netCents: Long)
data class Transfer(val fromUserId: Long, val toUserId: Long, val amountCents: Long)
data class CategoryBreakdown(val category: String, val paidCents: Long, val owedCents: Long)
data class MemberBreakdown(val userId: Long, val nickname: String, val categories: List<CategoryBreakdown>)
data class SettlementDto(
    val members: List<MemberBalance>,
    val transfers: List<Transfer>,
    val breakdown: List<MemberBreakdown>,
    val totalCents: Long,
    val expenseCount: Int,
)

// ---------- 通用 ----------
data class ErrorBody(val message: String?)

// ---------- WebSocket 事件 ----------
data class WsEvent(
    val type: String,
    val travelId: Long,
    val expenseId: Long? = null,
    val editId: Long? = null,
    val byUserId: Long? = null,
    val toUserId: Long? = null,
)
