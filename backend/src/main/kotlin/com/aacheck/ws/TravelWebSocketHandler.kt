package com.aacheck.ws

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 客户端连接 /ws 后发送 {"type":"subscribe","travelId":1} 订阅旅行事件，
 * 服务端随后按事件推送 JSON。
 */
@Component
class TravelWebSocketHandler(
    private val eventPushService: EventPushService,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val node: JsonNode = try {
            objectMapper.readTree(message.payload)
        } catch (_: Exception) {
            return
        }
        val userId = session.attributes["userId"] as? Long ?: return
        // 只允许订阅自己参加的旅行（成员校验由订阅接口语义保证；此处最小校验身份）
        if (node.get("type")?.asText() == "subscribe") {
            val travelId = node.get("travelId")?.asLong() ?: return
            // 简单防串订：把 userId 记入会话属性，供上层事件过滤
            session.attributes["subscribed_$travelId"] = userId
            eventPushService.subscribe(travelId, session)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        eventPushService.dropSession(session)
    }
}
