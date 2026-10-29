package com.aacheck.ws

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/** 推送给前端的事件信封 */
data class WsEvent(
    val type: String,
    val travelId: Long,
    val expenseId: Long? = null,
    val editId: Long? = null,
    val byUserId: Long? = null,
    val toUserId: Long? = null,
)

@Service
class EventPushService(
    private val objectMapper: ObjectMapper,
) {
    /** travelId -> 订阅该旅行的会话 */
    private val subscriptions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun subscribe(travelId: Long, session: WebSocketSession) {
        subscriptions.computeIfAbsent(travelId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unsubscribe(travelId: Long, session: WebSocketSession) {
        subscriptions[travelId]?.remove(session)
    }

    fun dropSession(session: WebSocketSession) {
        subscriptions.values.forEach { it.remove(session) }
    }

    /** 广播给订阅该旅行的所有在线会话 */
    fun push(travelId: Long, event: WsEvent) {
        val json = objectMapper.writeValueAsString(event)
        subscriptions[travelId]?.forEach { session ->
            try {
                if (session.isOpen) session.sendMessage(TextMessage(json))
            } catch (_: Exception) {
                dropSession(session)
            }
        }
    }
}
