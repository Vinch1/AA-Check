package com.aacheck.expense

import com.aacheck.common.badRequest
import com.aacheck.common.forbidden
import com.aacheck.common.notFound
import com.aacheck.security.requireUser
import com.aacheck.travel.TravelMemberRepository
import com.aacheck.travel.TravelRepository
import com.aacheck.ws.EventPushService
import com.aacheck.ws.WsEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class AccountEditRequest(
    val amountCents: Long? = null,
    val payerId: Long? = null,
    val category: ExpenseCategory? = null,
    val participantIds: List<Long>? = null,
) {
    fun isEmpty() = amountCents == null && payerId == null && category == null && participantIds == null
}

data class ExpenseUpsertRequest(
    val payerId: Long? = null,
    val amountCents: Long? = null,
    val category: ExpenseCategory? = null,
    val note: String? = null,
    val participantIds: List<Long>? = null,
    val imageUrls: List<String>? = null,
)

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val shareRepository: ExpenseShareRepository,
    private val imageRepository: ExpenseImageRepository,
    private val editRepository: ExpenseEditRepository,
    private val travelRepository: TravelRepository,
    private val memberRepository: TravelMemberRepository,
    private val eventPushService: EventPushService,
    private val objectMapper: ObjectMapper,
) {

    // ---------- 创建 ----------

    @Transactional
    fun create(travelId: Long, req: ExpenseUpsertRequest): Expense {
        val userId = requireUser()
        requireActiveMember(travelId, userId)
        val travel = travelRepository.findById(travelId).orElseThrow { notFound("旅行不存在") }
        if (travel.status == "ENDED") throw badRequest("旅行已结束，不能添加记录")

        val amount = req.amountCents ?: throw badRequest("金额不能为空")
        if (amount <= 0) throw badRequest("金额必须大于 0")
        val payerId = req.payerId ?: userId
        requireMembers(travelId, listOf(payerId))
        val participants = (req.participantIds ?: emptyList()).ifEmpty { listOf(payerId) }.distinct()
        requireMembers(travelId, participants)

        val expense = expenseRepository.save(
            Expense(
                travelId = travelId,
                authorId = userId,
                payerId = payerId,
                amountCents = amount,
                category = req.category ?: ExpenseCategory.OTHER,
                note = req.note?.trim()?.take(500) ?: "",
            )
        )
        writeShares(expense.id, amount, participants)
        saveImages(expense.id, req.imageUrls ?: emptyList())

        // 基线 = 作者创建时的版本
        expense.baselineJson = objectMapper.writeValueAsString(currentSnapshot(expense.id))
        expenseRepository.save(expense)

        push(travelId, "expense_created", expense.id, userId)
        return expense
    }

    // ---------- 查询 ----------

    @Transactional(readOnly = true)
    fun list(travelId: Long): List<ExpenseView> {
        val userId = requireUser()
        requireActiveMember(travelId, userId, allowEnded = true)
        return expenseRepository.findByTravelIdOrderByIdDesc(travelId).map { toView(it) }
    }

    @Transactional(readOnly = true)
    fun detail(expenseId: Long): ExpenseView {
        val userId = requireUser()
        val expense = expenseRepository.findById(expenseId).orElseThrow { notFound("记录不存在") }
        requireActiveMember(expense.travelId, userId, allowEnded = true)
        return toView(expense)
    }

    /** 当前用户作为作者的待追认修改（弹窗队列） */
    @Transactional(readOnly = true)
    fun myPendingApprovals(travelId: Long): List<PendingApproval> {
        val userId = requireUser()
        requireActiveMember(travelId, userId, allowEnded = true)
        val openEdits = editRepository.findByTravelIdAndStatusIn(travelId, listOf("PENDING", "DISPUTED"))
            .filter { it.status == "PENDING" } // DISPUTED 走横幅，不进队列
        return openEdits.mapNotNull { edit ->
            val expense = expenseRepository.findById(edit.expenseId).orElse(null) ?: return@mapNotNull null
            if (expense.authorId != userId) return@mapNotNull null
            PendingApproval(
                editId = edit.id,
                expenseId = expense.id,
                editorId = edit.editorId,
                before = readSnapshot(edit.beforeJson),
                after = readSnapshot(edit.afterJson),
                expense = toView(expense),
            )
        }
    }

    // ---------- 修改（乐观生效 + 追认） ----------

    @Transactional
    fun update(expenseId: Long, req: ExpenseUpsertRequest): ExpenseView {
        val userId = requireUser()
        val expense = expenseRepository.findById(expenseId).orElseThrow { notFound("记录不存在") }
        requireActiveMember(expense.travelId, userId)
        val travel = travelRepository.findById(expense.travelId).orElseThrow()
        if (travel.status == "ENDED") throw badRequest("旅行已结束，记录只读")

        val isAuthor = expense.authorId == userId
        val accountEdit = AccountEditRequest(req.amountCents, req.payerId, req.category, req.participantIds)

        if (!isAuthor) {
            // 非作者：备注/图片不可动
            if ((req.note != null && req.note.trim() != expense.note) || (req.imageUrls != null)) {
                throw forbidden("只有作者可以修改备注和图片")
            }
            if (accountEdit.isEmpty()) throw badRequest("没有可修改的内容")
            applyNonAuthorEdit(expense, userId, accountEdit)
        } else {
            applyAuthorEdit(expense, req, accountEdit)
        }
        push(expense.travelId, if (isAuthor) "expense_updated" else "expense_edited", expense.id, userId)
        return toView(expenseRepository.findById(expense.id).orElseThrow())
    }

    private fun applyAuthorEdit(expense: Expense, req: ExpenseUpsertRequest, edit: AccountEditRequest) {
        req.note?.trim()?.take(500)?.let { expense.note = it }
        req.imageUrls?.let { urls ->
            imageRepository.deleteByExpenseId(expense.id)
            saveImages(expense.id, urls)
        }
        if (edit.isEmpty()) {
            expense.updatedAt = Instant.now()
            return
        }
        applyAccountEdit(expense, edit)
        // 作者亲自改 = 新基线；未决修改作废（已被覆盖）
        editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED")).forEach {
            it.status = "WITHDRAWN"; it.resolvedAt = Instant.now()
        }
        expense.baselineJson = objectMapper.writeValueAsString(currentSnapshot(expense.id))
        expense.updatedAt = Instant.now()
    }

    private fun applyNonAuthorEdit(expense: Expense, userId: Long, edit: AccountEditRequest) {
        val beforeSnapshot = readSnapshot(expense.baselineJson)
        applyAccountEdit(expense, edit)
        val afterSnapshot = currentSnapshot(expense.id)
        // 聚合到同一条未决记录：before 保持最初基线，after 为最新值
        val open = editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED"))
        if (open.isEmpty()) {
            editRepository.save(
                ExpenseEdit(
                    expenseId = expense.id,
                    travelId = expense.travelId,
                    editorId = userId,
                    beforeJson = objectMapper.writeValueAsString(beforeSnapshot),
                    afterJson = objectMapper.writeValueAsString(afterSnapshot),
                    status = "PENDING",
                )
            )
        } else {
            val record = open.first()
            record.editorId = userId
            record.afterJson = objectMapper.writeValueAsString(afterSnapshot)
            if (record.status == "DISPUTED") record.status = "PENDING" // 新一轮修改重新待追认
            editRepository.save(record)
        }
        expense.updatedAt = Instant.now()
    }

    /** 把账务字段（金额/付款人/分类/参与人均摊）应用到记录上 */
    private fun applyAccountEdit(expense: Expense, edit: AccountEditRequest) {
        edit.payerId?.let { payerId ->
            requireMembers(expense.travelId, listOf(payerId))
            expense.payerId = payerId
        }
        edit.category?.let { expense.category = it }
        if (edit.amountCents != null || edit.participantIds != null) {
            val newAmount = edit.amountCents ?: expense.amountCents
            if (newAmount <= 0) throw badRequest("金额必须大于 0")
            val participants = (edit.participantIds ?: currentParticipants(expense.id)).distinct()
            if (participants.isEmpty()) throw badRequest("至少要有一个参与人")
            requireMembers(expense.travelId, participants)
            expense.amountCents = newAmount
            clearShares(expense.id)
            writeShares(expense.id, newAmount, participants)
        }
    }

    // ---------- 追认 / 撤回 ----------

    @Transactional
    fun approve(expenseId: Long) {
        val userId = requireUser()
        val expense = ownedExpenseForApproval(expenseId, userId)
        editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED")).forEach {
            it.status = "APPROVED"; it.resolvedAt = Instant.now()
        }
        expense.baselineJson = objectMapper.writeValueAsString(currentSnapshot(expense.id))
        expenseRepository.save(expense)
        push(expense.travelId, "expense_approved", expense.id, userId)
    }

    @Transactional
    fun dispute(expenseId: Long) {
        val userId = requireUser()
        val expense = ownedExpenseForApproval(expenseId, userId)
        val open = editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING"))
        if (open.isEmpty()) throw badRequest("没有待处理的修改")
        open.forEach { it.status = "DISPUTED"; it.resolvedAt = Instant.now() }
        push(expense.travelId, "expense_disputed", expense.id, userId)
    }

    @Transactional
    fun withdraw(expenseId: Long) {
        val userId = requireUser()
        val expense = expenseRepository.findById(expenseId).orElseThrow { notFound("记录不存在") }
        requireActiveMember(expense.travelId, userId, allowEnded = true)
        val open = editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED"))
        if (open.isEmpty()) throw badRequest("没有可撤回的修改")
        if (open.none { it.editorId == userId }) throw forbidden("只有修改人可以撤回")
        // 恢复到基线（作者认可的版本）
        val baseline = readSnapshot(expense.baselineJson)
        expense.amountCents = baseline.amountCents
        expense.payerId = baseline.payerId
        expense.category = baseline.category
        expense.updatedAt = Instant.now()
        clearShares(expense.id)
        baseline.shares.forEach { s ->
            shareRepository.save(ExpenseShare(expenseId = expense.id, userId = s.userId, shareCents = s.shareCents))
        }
        open.forEach { it.status = "WITHDRAWN"; it.resolvedAt = Instant.now() }
        expenseRepository.save(expense)
        push(expense.travelId, "expense_withdrawn", expense.id, userId)
    }

    // ---------- 删除（仅作者） ----------

    @Transactional
    fun delete(expenseId: Long) {
        val userId = requireUser()
        val expense = expenseRepository.findById(expenseId).orElseThrow { notFound("记录不存在") }
        requireActiveMember(expense.travelId, userId)
        if (expense.authorId != userId) throw forbidden("只有作者可以删除这条记录")
        shareRepository.deleteByExpenseId(expense.id)
        imageRepository.deleteByExpenseId(expense.id)
        editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED")).forEach {
            it.status = "WITHDRAWN"; it.resolvedAt = Instant.now()
        }
        expenseRepository.delete(expense)
        push(expense.travelId, "expense_deleted", expenseId, userId)
    }

    // ---------- 内部工具 ----------

    private fun ownedExpenseForApproval(expenseId: Long, userId: Long): Expense {
        val expense = expenseRepository.findById(expenseId).orElseThrow { notFound("记录不存在") }
        requireActiveMember(expense.travelId, userId, allowEnded = true)
        if (expense.authorId != userId) throw forbidden("只有原作者可以处理追认")
        return expense
    }

    private fun requireActiveMember(travelId: Long, userId: Long, allowEnded: Boolean = false) {
        if (!memberRepository.existsByTravelIdAndUserId(travelId, userId)) throw forbidden("你不是该旅行的成员")
        if (!allowEnded) {
            val travel = travelRepository.findById(travelId).orElseThrow { notFound("旅行不存在") }
            if (travel.status == "ENDED") throw badRequest("旅行已结束")
        }
    }

    private fun requireMembers(travelId: Long, userIds: List<Long>) {
        val members = memberRepository.findByTravelId(travelId).map { it.userId }.toSet()
        userIds.forEach { if (it !in members) throw badRequest("存在非旅行成员的参与人") }
    }

    /** 均摊：余数分给前几位（按 userId 排序保证稳定）。调用方需先删除旧分摊。 */
    private fun writeShares(expenseId: Long, amountCents: Long, participants: List<Long>) {
        val sorted = participants.distinct().sorted()
        val base = amountCents / sorted.size
        var remainder = (amountCents % sorted.size).toInt()
        sorted.forEach { uid ->
            val extra = if (remainder > 0) { remainder--; 1 } else 0
            shareRepository.save(ExpenseShare(expenseId = expenseId, userId = uid, shareCents = base + extra))
        }
    }

    /** 删除一条支出的全部分摊（按实体删除并立即 flush，避免与后续 insert 撞唯一键） */
    private fun clearShares(expenseId: Long) {
        val existing = shareRepository.findByExpenseId(expenseId)
        if (existing.isNotEmpty()) {
            shareRepository.deleteAll(existing)
            shareRepository.flush()
        }
    }

    private fun saveImages(expenseId: Long, urls: List<String>) {
        urls.filter { it.isNotBlank() }.take(2).forEachIndexed { i, url ->
            imageRepository.save(ExpenseImage(expenseId = expenseId, url = url, sortOrder = i))
        }
    }

    private fun currentParticipants(expenseId: Long): List<Long> =
        shareRepository.findByExpenseId(expenseId).map { it.userId }

    private fun currentSnapshot(expenseId: Long): AccountSnapshot {
        val expense = expenseRepository.findById(expenseId).orElseThrow()
        val shares = shareRepository.findByExpenseId(expenseId)
            .sortedBy { it.userId }
            .map { AccountSnapshot.ShareLine(it.userId, it.shareCents) }
        return AccountSnapshot(expense.amountCents, expense.payerId, expense.category, shares)
    }

    private fun readSnapshot(json: String): AccountSnapshot =
        objectMapper.readValue(json, AccountSnapshot::class.java)

    private fun push(travelId: Long, type: String, expenseId: Long, byUserId: Long) {
        eventPushService.push(travelId, WsEvent(type = type, travelId = travelId, expenseId = expenseId, byUserId = byUserId))
    }

    private fun toView(expense: Expense): ExpenseView {
        val shares = shareRepository.findByExpenseId(expense.id).sortedBy { it.userId }
            .map { ShareLine(it.userId, it.shareCents) }
        val images = imageRepository.findByExpenseIdOrderBySortOrder(expense.id).map { it.url }
        val openEdit = editRepository.findByExpenseIdAndStatusIn(expense.id, listOf("PENDING", "DISPUTED"))
            .firstOrNull()?.let { edit ->
                OpenEditView(
                    editId = edit.id,
                    editorId = edit.editorId,
                    status = edit.status,
                    before = readSnapshot(edit.beforeJson),
                    after = readSnapshot(edit.afterJson),
                )
            }
        return ExpenseView(
            id = expense.id,
            travelId = expense.travelId,
            authorId = expense.authorId,
            payerId = expense.payerId,
            amountCents = expense.amountCents,
            category = expense.category,
            note = expense.note,
            createdAt = expense.createdAt,
            updatedAt = expense.updatedAt,
            shares = shares,
            images = images,
            openEdit = openEdit,
        )
    }
}

data class ShareLine(val userId: Long, val shareCents: Long)

data class OpenEditView(
    val editId: Long,
    val editorId: Long,
    val status: String,
    val before: AccountSnapshot,
    val after: AccountSnapshot,
)

data class PendingApproval(
    val editId: Long,
    val expenseId: Long,
    val editorId: Long,
    val before: AccountSnapshot,
    val after: AccountSnapshot,
    val expense: ExpenseView,
)

data class ExpenseView(
    val id: Long,
    val travelId: Long,
    val authorId: Long,
    val payerId: Long,
    val amountCents: Long,
    val category: ExpenseCategory,
    val note: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val shares: List<ShareLine>,
    val images: List<String>,
    val openEdit: OpenEditView?,
)
