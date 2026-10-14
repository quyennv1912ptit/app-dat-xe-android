package com.example.app_dat_xe.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicInteger

/** Minimal STOMP 1.2 client for the Spring broker endpoints used by the ride backend. */
class RideStompClient {
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val subscriptionId = AtomicInteger(1)
    private var socket: WebSocket? = null
    @Volatile private var connected = false
    private var reconnectEnabled = false
    private var reconnectTask: Runnable? = null
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    private var firebaseIdToken: String? = null
    private var heartbeatTask: Runnable? = null
    private var destinations: Set<String> = emptySet()
    private var payloadListener: ((String, String) -> Unit)? = null
    private var stateListener: ((Boolean, String?) -> Unit)? = null

    @Synchronized
    fun connect(
        firebaseIdToken: String,
        destinations: Set<String>,
        onPayload: (destination: String, payload: String) -> Unit,
        onStateChanged: (connected: Boolean, error: String?) -> Unit = { _, _ -> }
    ) {
        this.destinations = destinations
        this.payloadListener = onPayload
        this.stateListener = onStateChanged
        this.firebaseIdToken = firebaseIdToken
        reconnectEnabled = true
        reconnectTask?.let(mainHandler::removeCallbacks)
        reconnectTask = null
        if (socket == null) openSocket(firebaseIdToken)
    }

    @Synchronized
    private fun openSocket(token: String) {
        if (!reconnectEnabled || socket != null) return
        val request = Request.Builder()
            .url(RetrofitClient.STOMP_URL)
            .header("Authorization", "Bearer $token")
            .header("Sec-WebSocket-Protocol", "v12.stomp")
            .build()
        socket = RetrofitClient.webSocketClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(
                    "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n" +
                        "Authorization:Bearer $token\n\n\u0000"
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                text.split('\u0000').forEach(::handleFrame)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                synchronized(this@RideStompClient) {
                    if (socket === webSocket) socket = null
                    connected = false
                    stopHeartbeats()
                    stateListener?.invoke(false, response?.message ?: t.message)
                    scheduleReconnect()
                }
                Log.w("RIDE_STOMP", "WebSocket STOMP thất bại", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                synchronized(this@RideStompClient) {
                    if (socket === webSocket) socket = null
                    connected = false
                    stopHeartbeats()
                    stateListener?.invoke(false, reason)
                    scheduleReconnect()
                }
            }
        })
    }

    @Synchronized
    fun send(destination: String, payload: Any): Boolean {
        if (!connected) return false
        val body = if (payload is String) payload else gson.toJson(payload)
        val length = body.toByteArray(Charsets.UTF_8).size
        return socket?.send(
            "SEND\ndestination:$destination\ncontent-type:application/json\n" +
                "content-length:$length\n\n$body\u0000"
        ) == true
    }

    @Synchronized
    fun isConnected(): Boolean = connected

    fun sendDriverLocation(driverId: Long, latitude: Double, longitude: Double): Boolean =
        send(
            "/app/location",
            DriverLocationFrame(driverId, GeoPointFrame(latitude, longitude))
        )

    @Synchronized
    fun disconnect() {
        reconnectEnabled = false
        reconnectTask?.let(mainHandler::removeCallbacks)
        reconnectTask = null
        if (connected) socket?.send("DISCONNECT\n\n\u0000")
        connected = false
        stopHeartbeats()
        socket?.close(1000, "client disconnect")
        socket = null
        firebaseIdToken = null
        payloadListener = null
        stateListener = null
    }

    private fun handleFrame(rawFrame: String) {
        val parsed = parseStompFrame(rawFrame) ?: return
        when (parsed.command) {
            "CONNECTED" -> {
                connected = true
                reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
                destinations.forEach { destination ->
                    socket?.send(
                        "SUBSCRIBE\nid:${subscriptionId.getAndIncrement()}\n" +
                            "destination:$destination\nack:auto\n\n\u0000"
                    )
                }
                stateListener?.invoke(true, null)
                startHeartbeats()
            }
            "MESSAGE" -> parsed.headers["destination"]?.let { payloadListener?.invoke(it, parsed.body) }
            "ERROR" -> {
                connected = false
                stateListener?.invoke(false, parsed.body.ifBlank { "Lỗi STOMP từ máy chủ" })
                socket?.close(1000, "stomp error")
            }
        }
    }

    private fun startHeartbeats() {
        stopHeartbeats()
        val task = object : Runnable {
            override fun run() {
                if (connected) {
                    socket?.send("\n")
                    mainHandler.postDelayed(this, 10_000)
                }
            }
        }
        heartbeatTask = task
        mainHandler.postDelayed(task, 10_000)
    }

    private fun stopHeartbeats() {
        heartbeatTask?.let(mainHandler::removeCallbacks)
        heartbeatTask = null
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (!reconnectEnabled || reconnectTask != null) return
        val waitMs = reconnectDelayMs
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        val task = Runnable {
            synchronized(this) {
                reconnectTask = null
                if (reconnectEnabled && socket == null) {
                    firebaseIdToken?.let(::openSocket)
                }
            }
        }
        reconnectTask = task
        mainHandler.postDelayed(task, waitMs)
    }

    companion object {
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }
}

data class DriverLocationFrame(val driverId: Long, val location: GeoPointFrame)
data class GeoPointFrame(val lat: Double, val lng: Double)
