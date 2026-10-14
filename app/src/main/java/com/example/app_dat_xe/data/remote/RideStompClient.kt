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
    private var connected = false
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
        if (socket != null) return
        this.destinations = destinations
        this.payloadListener = onPayload
        this.stateListener = onStateChanged
        val request = Request.Builder()
            .url(RetrofitClient.STOMP_URL)
            .header("Authorization", "Bearer $firebaseIdToken")
            .header("Sec-WebSocket-Protocol", "v12.stomp")
            .build()
        socket = RetrofitClient.webSocketClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(
                    "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n" +
                        "Authorization:Bearer $firebaseIdToken\n\n\u0000"
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                text.split('\u0000').forEach(::handleFrame)
            }

            override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
                connected = false
                socket = null
                stopHeartbeats()
                stateListener?.invoke(false, response?.message ?: throwable.message)
                Log.w("RIDE_STOMP", "WebSocket STOMP thất bại", throwable)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                socket = null
                stopHeartbeats()
                stateListener?.invoke(false, reason)
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
        if (connected) socket?.send("DISCONNECT\n\n\u0000")
        connected = false
        stopHeartbeats()
        socket?.close(1000, "client disconnect")
        socket = null
        payloadListener = null
        stateListener = null
    }

    private fun handleFrame(rawFrame: String) {
        val frame = rawFrame.trim('\r', '\n')
        if (frame.isBlank()) return
        val lines = frame.split('\n')
        val command = lines.firstOrNull()?.trim().orEmpty()
        val headerEnd = lines.indexOfFirst { it.isBlank() }
        val headers = if (headerEnd > 0) {
            lines.subList(1, headerEnd).mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }.toMap()
        } else emptyMap()
        val body = if (headerEnd >= 0 && headerEnd + 1 < lines.size) {
            lines.subList(headerEnd + 1, lines.size).joinToString("\n")
        } else ""

        when (command) {
            "CONNECTED" -> {
                connected = true
                destinations.forEach { destination ->
                    socket?.send(
                        "SUBSCRIBE\nid:${subscriptionId.getAndIncrement()}\n" +
                            "destination:$destination\nack:auto\n\n\u0000"
                    )
                }
                stateListener?.invoke(true, null)
                startHeartbeats()
            }
            "MESSAGE" -> headers["destination"]?.let { payloadListener?.invoke(it, body) }
            "ERROR" -> stateListener?.invoke(false, body.ifBlank { "Lỗi STOMP từ máy chủ" })
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
}

data class DriverLocationFrame(val driverId: Long, val location: GeoPointFrame)
data class GeoPointFrame(val lat: Double, val lng: Double)
