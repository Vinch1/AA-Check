package com.aacheck.travel

import com.aacheck.common.badRequest
import com.aacheck.common.forbidden
import com.aacheck.common.notFound
import com.aacheck.expense.ExpenseEditRepository
import com.aacheck.expense.ExpenseImageRepository
import com.aacheck.expense.ExpenseRepository
import com.aacheck.expense.ExpenseShareRepository
import com.aacheck.security.requireUser
import com.aacheck.user.UserRepository
import com.aacheck.ws.EventPushService
import com.aacheck.ws.WsEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.Instant

@Service
class TravelService(
    private val travelRepository: TravelRepository,
    private val memberRepository: TravelMemberRepository,
    private val userRepository: UserRepository,
    private val eventPushService: EventPushService,
    private val expenseRepository: ExpenseRepository,
    private val shareRepository: ExpenseShareRepository,
    private val imageRepository: ExpenseImageRepository,
    private val editRepository: ExpenseEditRepository,
    @Value("\${app.upload-dir}") private val uploadDir: String,
) {
    private val random = SecureRandom()
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 去掉易混淆的 I O 0 1

    @Transactional
    fun create(name: String, note: String?): Travel {
        val userId = requireUser()
        if (name.isBlank()) throw badRequest("旅行名称不能为空")
        val travel = travelRepository.save(
            Travel(name = name.trim(), note = note?.trim()?.takeIf { it.isNotEmpty() }, creatorId = userId)
        )
        travel.inviteCode = generateUniqueCode()
        memberRepository.save(TravelMember(travelId = travel.id, userId = userId))
        return travelRepository.save(travel)
    }

    @Transactional
    fun join(code: String): Travel {
        val userId = requireUser()
        val travel = travelRepository.findByInviteCode(code.trim().uppercase())
            ?: throw notFound("邀请码无效")
        if (travel.status == "ENDED") throw badRequest("旅行已结束，无法加入")
        if (!memberRepository.existsByTravelIdAndUserId(travel.id, userId)) {
            memberRepository.save(TravelMember(travelId = travel.id, userId = userId))
            eventPushService.push(travel.id, WsEvent(type = "travel_updated", travelId = travel.id, byUserId = userId))
        }
        return travel
    }

    @Transactional
    fun regenerateInviteCode(travelId: Long): Travel {
        val userId = requireUser()
        val travel = getOwnedTravel(travelId, userId)
        travel.inviteCode = generateUniqueCode()
        return travelRepository.save(travel)
    }

    @Transactional
    fun end(travelId: Long): Travel {
        val userId = requireUser()
        val travel = getOwnedTravel(travelId, userId)
        if (travel.status == "ENDED") throw badRequest("旅行已结束")
        travel.status = "ENDED"
        travel.endedAt = Instant.now()
        eventPushService.push(travel.id, WsEvent(type = "travel_ended", travelId = travel.id, byUserId = userId))
        return travelRepository.save(travel)
    }

    @Transactional
    fun delete(travelId: Long) {
        val userId = requireUser()
        val travel = getOwnedTravel(travelId, userId) // 不存在或非创建者都会抛异常
        val expenses = expenseRepository.findByTravelId(travelId)
        val expenseIds = expenses.map { it.id }
        if (expenseIds.isNotEmpty()) {
            // 清理磁盘上的图片文件（best-effort）
            val urls = imageRepository.findByExpenseIdIn(expenseIds).map { it.url }
            deleteImageFiles(urls)
            editRepository.deleteByExpenseIdIn(expenseIds)
            imageRepository.deleteByExpenseIdIn(expenseIds)
            shareRepository.deleteByExpenseIdIn(expenseIds)
            expenseRepository.deleteAll(expenses)
        }
        memberRepository.deleteByTravelId(travelId)
        travelRepository.delete(travel)
        eventPushService.push(travelId, WsEvent(type = "travel_deleted", travelId = travelId, byUserId = userId))
    }

    private fun deleteImageFiles(urls: List<String>) {
        val dir = Paths.get(uploadDir).toAbsolutePath().normalize()
        for (url in urls) {
            val name = url.substringAfterLast('/')
            if (name.isBlank()) continue
            runCatching { Files.deleteIfExists(dir.resolve(name)) }
        }
    }

    @Transactional
    fun update(travelId: Long, name: String?, note: String?): Travel {
        val userId = requireUser()
        val travel = getOwnedTravel(travelId, userId)
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { travel.name = it }
        if (note != null) travel.note = note.trim().takeIf { it.isNotEmpty() }
        return travelRepository.save(travel)
    }

    fun myTravels(): List<TravelWithMembers> {
        val userId = requireUser()
        val memberships = memberRepository.findByUserId(userId)
        return memberships.map { m ->
            val travel = travelRepository.findById(m.travelId).orElseThrow()
            TravelWithMembers(travel, memberRepository.findByTravelId(travel.id).mapNotNull { mm ->
                userRepository.findById(mm.userId).orElse(null)?.let { MemberBrief(it.id, it.nickname, it.avatarUrl, it.id == travel.creatorId) }
            })
        }.sortedWith(compareBy({ it.travel.status == "ENDED" }, { it.travel.createdAt }))
    }

    fun detail(travelId: Long): TravelDetail {
        val userId = requireUser()
        requireMember(travelId, userId)
        val travel = travelRepository.findById(travelId).orElseThrow()
        val members = memberRepository.findByTravelId(travelId).mapNotNull { m ->
            userRepository.findById(m.userId).orElse(null)?.let {
                MemberBrief(it.id, it.nickname, it.avatarUrl, it.id == travel.creatorId)
            }
        }
        return TravelDetail(travel, members, userId == travel.creatorId)
    }

    fun requireMember(travelId: Long, userId: Long) {
        if (!memberRepository.existsByTravelIdAndUserId(travelId, userId)) {
            throw forbidden("你不是该旅行的成员")
        }
    }

    private fun getOwnedTravel(travelId: Long, userId: Long): Travel {
        val travel = travelRepository.findById(travelId).orElseThrow { notFound("旅行不存在") }
        if (travel.creatorId != userId) throw forbidden("只有创建者可以执行此操作")
        return travel
    }

    private fun generateUniqueCode(): String {
        var code: String
        do {
            code = (1..6).map { codeChars[random.nextInt(codeChars.length)] }.joinToString("")
        } while (travelRepository.existsByInviteCode(code))
        return code
    }
}

data class MemberBrief(val userId: Long, val nickname: String, val avatarUrl: String?, val isCreator: Boolean)
data class TravelWithMembers(val travel: Travel, val members: List<MemberBrief>)
data class TravelDetail(val travel: Travel, val members: List<MemberBrief>, val amCreator: Boolean)
