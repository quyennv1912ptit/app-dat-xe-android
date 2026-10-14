package com.example.app_dat_xe.tracking

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.google.gson.JsonParser
import java.util.LinkedHashMap

object RideEventDeduplicator {
    private const val MAX_IDS = 512
    private const val RETENTION_MS = 10 * 60 * 1_000L
    private val seen = LinkedHashMap<String, Long>()

    @Synchronized
    fun firstTime(messageId: String?): Boolean {
        if (messageId.isNullOrBlank()) return true
        val now = System.currentTimeMillis()
        seen.entries.removeAll { now - it.value > RETENTION_MS }
        if (seen.containsKey(messageId)) return false
        seen[messageId] = now
        while (seen.size > MAX_IDS) seen.remove(seen.keys.first())
        return true
    }
}

data class RideStompEvent(val destination: String, val payload: String)

object RideEventBus {
    private val mutableEvents = MutableSharedFlow<RideStompEvent>(extraBufferCapacity = 32)
    val events = mutableEvents.asSharedFlow()

    fun publish(destination: String, payload: String): Boolean {
        val messageId = try {
            JsonParser.parseString(payload).asJsonObject.get("messageId")?.takeUnless { it.isJsonNull }?.asString
        } catch (_: Exception) {
            null
        }
        if (!RideEventDeduplicator.firstTime(messageId)) return false
        return mutableEvents.tryEmit(RideStompEvent(destination, payload))
    }
}
