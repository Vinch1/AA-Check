package com.aacheck.app.data

import com.aacheck.app.core.ServerConfig
import com.aacheck.app.core.TokenStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WsClient(private val tokenStore: TokenStore) {
    private val gson = Gson()
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private var socket: WebSocket? = null
    private var subscribedTravelId: Long? = null

    fun connect() {
        val token = tokenStore.token ?: return
        val url = ServerConfig.wsUrl + "?token=" + token
        val request = Request.Builder().url(url).build()
        socket = OkHttpClient.Builder().build().newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    _events.tryEmit(gson.fromJson(text, WsEvent::class.java))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // 断线后交由上层按需重连
            }
        })
    }

    fun subscribe(travelId: Long) {
        subscribedTravelId = travelId
        socket?.send(gson.toJson(mapOf("type" to "subscribe", "travelId" to travelId)))
    }

    fun disconnect() {
        subscribedTravelId = null
        socket?.close(1000, null)
        socket = null
    }
}
