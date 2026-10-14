package com.example.app_dat_xe.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object DeviceLocationProvider {
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? = try {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .await()
    } catch (_: Exception) {
        null
    }
}
