package com.aacheck.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aacheck.app.core.ServerConfig
import com.aacheck.app.core.TokenStore
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WsClient(private val tokenStore: TokenStore) {
    private companion object {
        const val TAG = "WsClient"
        const val MAX_RECONNECT_DELAY_SECONDS = 30L
    }

    private val gson = Gson()
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null
    private var subscribedTravelId: Long? = null
    private var isConnected = false
    private var shouldReconnect = false
    private var reconnectAttempt = 0
    private var connectionGeneration = 0L
    private var reconnectRunnable: Runnable? = null

    fun connect() {
        val token = tokenStore.token?.takeIf(String::isNotBlank) ?: return
        shouldReconnect = true
        reconnectAttempt = 0
        connectionGeneration += 1
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectRunnable = null
        socket?.cancel()
        socket = null
        isConnected = false
        openSocket(token, connectionGeneration)
    }

    private fun openSocket(token: String, generation: Long) {
        val url = ServerConfig.wsUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("token", token)
            ?.build()
            ?: throw IllegalArgumentException("Invalid WebSocket URL: ${ServerConfig.wsUrl}")
        val request = Request.Builder().url(url).build()
        val newSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                mainHandler.post {
                    if (!isCurrent(webSocket, generation)) return@post
                    isConnected = true
                    reconnectAttempt = 0
                    subscribedTravelId?.let { sendSubscription(webSocket, it, generation) }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = try {
                    gson.fromJson(text, WsEvent::class.java)
                } catch (error: JsonParseException) {
                    Log.w(TAG, "Ignoring malformed WebSocket event", error)
                    return
                }
                if (event != null) _events.tryEmit(event)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                mainHandler.post { handleDisconnect(webSocket, generation, t) }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                mainHandler.post { handleDisconnect(webSocket, generation, null) }
            }
        })
        socket = newSocket
    }

    fun subscribe(travelId: Long) {
        subscribedTravelId = travelId
        if (isConnected) {
            socket?.let { sendSubscription(it, travelId, connectionGeneration) }
        }
    }

    private fun sendSubscription(webSocket: WebSocket, travelId: Long, generation: Long) {
        val message = gson.toJson(mapOf("type" to "subscribe", "travelId" to travelId))
        if (!webSocket.send(message)) {
            handleDisconnect(webSocket, generation, null)
        }
    }

    private fun isCurrent(webSocket: WebSocket, generation: Long): Boolean =
        generation == connectionGeneration && socket === webSocket

    private fun handleDisconnect(webSocket: WebSocket, generation: Long, error: Throwable?) {
        if (!isCurrent(webSocket, generation)) return
        socket = null
        isConnected = false
        error?.let { Log.w(TAG, "WebSocket disconnected; reconnecting", it) }
        if (!shouldReconnect) return

        val delay = (1L shl reconnectAttempt.coerceAtMost(5))
            .coerceAtMost(MAX_RECONNECT_DELAY_SECONDS)
        reconnectAttempt += 1
        reconnectRunnable = Runnable {
            reconnectRunnable = null
            val token = tokenStore.token?.takeIf(String::isNotBlank) ?: return@Runnable
            if (shouldReconnect && generation == connectionGeneration && socket == null) {
                openSocket(token, generation)
            }
        }.also { mainHandler.postDelayed(it, TimeUnit.SECONDS.toMillis(delay)) }
    }

    fun disconnect() {
        shouldReconnect = false
        connectionGeneration += 1
        reconnectRunnable?.let(mainHandler::removeCallbacks)
        reconnectRunnable = null
        subscribedTravelId = null
        isConnected = false
        socket?.close(1000, null)
        socket = null
    }
}
