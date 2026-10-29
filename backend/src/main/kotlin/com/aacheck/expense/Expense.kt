package com.aacheck.expense

import jakarta.persistence.*
import java.time.Instant

enum class ExpenseCategory {
    FOOD, TRANSPORT, TICKET, HOTEL, SHOPPING, ENTERTAINMENT, OTHER
}

/**
 * 一笔支出的可编辑账务字段的快照（金额/付款人/分类/分摊），
 * 用于"原始 vs 当前"对比与撤回恢复。
 */
data class AccountSnapshot(
    val amountCents: Long,
    val payerId: Long,
    val category: ExpenseCategory,
    val shares: List<ShareLine>,
) {
    data class ShareLine(val userId: Long, val shareCents: Long)
}

@Entity
@Table(name = "expenses")
class Expense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val travelId: Long = 0,

    /** 记录的作者（最初写这条记录的人） */
    @Column(nullable = false)
    val authorId: Long = 0,

    @Column(nullable = false)
    var payerId: Long = 0,

    /** 金额，单位：分 */
    @Column(nullable = false)
    var amountCents: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var category: ExpenseCategory = ExpenseCategory.OTHER,

    @Column(length = 512)
    var note: String = "",

    val createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now(),

    /**
     * 基线快照（作者认可版本）的 JSON。
     * 非作者修改乐观生效后，与当前值形成"原始 vs 当前"对比；
     * 作者同意 -> 基线更新为当前值；撤回 -> 用基线恢复。
     */
    @Column(nullable = false, columnDefinition = "text")
    var baselineJson: String = "",
)

@Entity
@Table(
    name = "expense_shares",
    uniqueConstraints = [UniqueConstraint(columnNames = ["expenseId", "userId"])],
)
class ExpenseShare(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val expenseId: Long = 0,

    @Column(nullable = false)
    val userId: Long = 0,

    /** 分摊金额，单位：分 */
    @Column(nullable = false)
    var shareCents: Long = 0,
)

@Entity
@Table(name = "expense_images")
class ExpenseImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val expenseId: Long = 0,

    @Column(nullable = false, length = 256)
    val url: String = "",

    val sortOrder: Int = 0,
)

/**
 * 非作者对账务字段（金额/参与人/分类）的修改记录。
 * status: PENDING(待作者追认) / APPROVED(作者已同意) / DISPUTED(作者不同意,待线下沟通) / WITHDRAWN(修改人已撤回)
 */
@Entity
@Table(name = "expense_edits")
class ExpenseEdit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val expenseId: Long = 0,

    @Column(nullable = false)
    val travelId: Long = 0,

    /** 最后一位修改人 */
    @Column(nullable = false)
    var editorId: Long = 0,

    /** 修改时的基线（原始）快照 */
    @Column(nullable = false, columnDefinition = "text")
    var beforeJson: String = "",

    /** 本次修改后的快照 */
    @Column(nullable = false, columnDefinition = "text")
    var afterJson: String = "",

    @Column(nullable = false, length = 16)
    var status: String = "PENDING",

    val createdAt: Instant = Instant.now(),

    var resolvedAt: Instant? = null,
)

interface ExpenseRepository : org.springframework.data.jpa.repository.JpaRepository<Expense, Long> {
    fun findByTravelIdOrderByIdDesc(travelId: Long): List<Expense>
    fun findByTravelId(travelId: Long): List<Expense>
}

interface ExpenseShareRepository : org.springframework.data.jpa.repository.JpaRepository<ExpenseShare, Long> {
    fun findByExpenseId(expenseId: Long): List<ExpenseShare>
    fun deleteByExpenseId(expenseId: Long)
    fun findByExpenseIdIn(expenseIds: List<Long>): List<ExpenseShare>
    fun deleteByExpenseIdIn(expenseIds: List<Long>)
}

interface ExpenseImageRepository : org.springframework.data.jpa.repository.JpaRepository<ExpenseImage, Long> {
    fun findByExpenseIdOrderBySortOrder(expenseId: Long): List<ExpenseImage>
    fun deleteByExpenseId(expenseId: Long)
    fun findByExpenseIdIn(expenseIds: List<Long>): List<ExpenseImage>
    fun deleteByExpenseIdIn(expenseIds: List<Long>)
}

interface ExpenseEditRepository : org.springframework.data.jpa.repository.JpaRepository<ExpenseEdit, Long> {
    fun findByExpenseIdAndStatusIn(expenseId: Long, statuses: List<String>): List<ExpenseEdit>
    fun findByTravelIdAndStatusIn(travelId: Long, statuses: List<String>): List<ExpenseEdit>
    fun deleteByExpenseIdIn(expenseIds: List<Long>)
}
