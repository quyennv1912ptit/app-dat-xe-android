package com.example.app_dat_xe.feature.ride

fun stepFromStatus(status: String): Int = when (status) {
    "DRIVER_ASSIGNED" -> 0
    "DRIVER_ARRIVING" -> 1
    "DRIVER_ARRIVED" -> 2
    "VEHICLE_HANDOVER" -> 3
    "IN_PROGRESS" -> 4
    "ARRIVED_DESTINATION" -> 5
    "VEHICLE_RETURNED" -> 6
    else -> 0
}

fun formatVnd(amount: Long?): String = amount?.toString()
    ?.reversed()
    ?.chunked(3)
    ?.joinToString(".") { it.reversed() }
    ?.reversed()
    ?: "–"

val activeRideStatuses = setOf(
    "PENDING", "SEARCHING", "DRIVER_ASSIGNED", "DRIVER_ARRIVING", "DRIVER_ARRIVED",
    "VEHICLE_HANDOVER", "IN_PROGRESS", "ARRIVED_DESTINATION", "VEHICLE_RETURNED"
)
