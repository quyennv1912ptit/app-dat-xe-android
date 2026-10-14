package com.example.app_dat_xe.tracking

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class RideStompEvent(val destination: String, val payload: String)

object RideEventBus {
    private val mutableEvents = MutableSharedFlow<RideStompEvent>(extraBufferCapacity = 32)
    val events = mutableEvents.asSharedFlow()

    fun publish(destination: String, payload: String) {
        mutableEvents.tryEmit(RideStompEvent(destination, payload))
    }
}
