package com.aacheck.config

import com.aacheck.security.JwtService
import com.aacheck.ws.TravelWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val handler: TravelWebSocketHandler,
    private val jwtService: JwtService,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws")
            .addInterceptors(object : HandshakeInterceptor {
                override fun beforeHandshake(
                    request: ServerHttpRequest,
                    response: ServerHttpResponse,
                    wsHandler: WebSocketHandler,
                    attributes: MutableMap<String, Any>,
                ): Boolean {
                    val token = (request as? ServletServerHttpRequest)
                        ?.servletRequest?.getParameter("token")
                        ?: request.headers.getFirst("Authorization")?.removePrefix("Bearer ")
                    val userId = token?.let { jwtService.parseUserId(it.trim()) }
                    return if (userId != null) {
                        attributes["userId"] = userId
                        true
                    } else {
                        false
                    }
                }

                override fun afterHandshake(
                    request: ServerHttpRequest,
                    response: ServerHttpResponse,
                    wsHandler: WebSocketHandler,
                    exception: Exception?,
                ) {
                }
            })
    }
}
