package com.aacheck.expense

import com.aacheck.common.forbidden
import com.aacheck.common.notFound
import com.aacheck.security.requireUser
import com.aacheck.travel.TravelMemberRepository
import com.aacheck.travel.TravelRepository
import com.aacheck.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class MemberBalance(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String?,
    /** 正数 = 应收（垫付多于分摊），负数 = 应付 */
    val netCents: Long,
)

data class Transfer(val fromUserId: Long, val toUserId: Long, val amountCents: Long)

data class CategoryBreakdown(
    val category: ExpenseCategory,
    /** 该成员在此分类的垫付总额 */
    val paidCents: Long,
    /** 该成员在此分类的分摊总额 */
    val owedCents: Long,
)

data class MemberBreakdown(
    val userId: Long,
    val nickname: String,
    val categories: List<CategoryBreakdown>,
)

data class SettlementDto(
    val members: List<MemberBalance>,
    val transfers: List<Transfer>,
    val breakdown: List<MemberBreakdown>,
    val totalCents: Long,
    val expenseCount: Int,
)

@Service
class SettlementService(
    private val travelRepository: TravelRepository,
    private val memberRepository: TravelMemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val shareRepository: ExpenseShareRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun settlement(travelId: Long): SettlementDto {
        val userId = requireUser()
        if (!memberRepository.existsByTravelIdAndUserId(travelId, userId)) throw forbidden("你不是该旅行的成员")
        travelRepository.findById(travelId).orElseThrow { notFound("旅行不存在") }

        val members = memberRepository.findByTravelId(travelId).mapNotNull { m ->
            userRepository.findById(m.userId).orElse(null)?.let { Triple(it.id, it.nickname, it.avatarUrl) }
        }
        val memberIds = members.map { it.first }.toSet()

        val expenses = expenseRepository.findByTravelId(travelId)
        val allShares = shareRepository.findByExpenseIdIn(expenses.map { it.id })
        val expenseOf = expenses.associateBy { it.id }

        val net = memberIds.associateWith { 0L }.toMutableMap()
        val paidByCat = memberIds.associateWith { mutableMapOf<ExpenseCategory, Long>() }
        val owedByCat = memberIds.associateWith { mutableMapOf<ExpenseCategory, Long>() }

        expenses.forEach { e ->
            if (net.containsKey(e.payerId)) {
                net[e.payerId] = net[e.payerId]!! + e.amountCents
                paidByCat[e.payerId]!!.merge(e.category, e.amountCents, Long::plus)
            }
        }
        allShares.forEach { s ->
            val expense = expenseOf[s.expenseId] ?: return@forEach
            if (net.containsKey(s.userId)) {
                net[s.userId] = net[s.userId]!! - s.shareCents
                owedByCat[s.userId]!!.merge(expense.category, s.shareCents, Long::plus)
            }
        }

        val balances = members
            .map { (id, name, avatar) -> MemberBalance(id, name, avatar, net[id] ?: 0) }
            .sortedByDescending { it.netCents }

        val breakdown = members.map { (id, name, _) ->
            MemberBreakdown(
                userId = id,
                nickname = name,
                categories = ExpenseCategory.entries.map { cat ->
                    CategoryBreakdown(
                        category = cat,
                        paidCents = paidByCat[id]?.get(cat) ?: 0,
                        owedCents = owedByCat[id]?.get(cat) ?: 0,
                    )
                }.filter { it.paidCents != 0L || it.owedCents != 0L },
            )
        }

        return SettlementDto(
            members = balances,
            transfers = planTransfers(net),
            breakdown = breakdown,
            totalCents = expenses.sumOf { it.amountCents },
            expenseCount = expenses.size,
        )
    }

    /** 最少转账（贪心）：最大应收配最大应付 */
    private fun planTransfers(net: Map<Long, Long>): List<Transfer> {
        data class Node(val userId: Long, var amount: Long)

        val creditors = net.filter { it.value > 0 }.map { Node(it.key, it.value) }
        val debtors = net.filter { it.value < 0 }.map { Node(it.key, -it.value) }
        val result = mutableListOf<Transfer>()

        var ci = 0
        var di = 0
        while (ci < creditors.size && di < debtors.size) {
            val c = creditors[ci]
            val d = debtors[di]
            val amount = minOf(c.amount, d.amount)
            if (amount > 0) result.add(Transfer(fromUserId = d.userId, toUserId = c.userId, amountCents = amount))
            c.amount -= amount
            d.amount -= amount
            if (c.amount == 0L) ci++
            if (d.amount == 0L) di++
        }
        return result
    }
}
