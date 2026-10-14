package com.example.app_dat_xe.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.app_dat_xe.R
import com.example.app_dat_xe.data.location.DeviceLocationProvider
import com.example.app_dat_xe.data.remote.RideStompClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DriverLocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var stompClient: RideStompClient? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val driverId = intent?.getLongExtra(EXTRA_DRIVER_ID, 0L)
            ?: getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE).getString("id", null)?.toLongOrNull()
            ?: 0L
        if (driverId <= 0L) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        if (serviceScope.isActive && stompClient == null) {
            serviceScope.launch { trackDriver(driverId) }
        }
        return START_STICKY
    }

    private suspend fun trackDriver(driverId: Long) {
        while (serviceScope.isActive) {
            try {
                var client = stompClient
                if (client?.isConnected() != true) {
                    client?.disconnect()
                    val token = FirebaseAuth.getInstance().currentUser
                        ?.getIdToken(false)?.await()?.token
                    if (!token.isNullOrBlank()) {
                        client = RideStompClient().also { newClient ->
                            stompClient = newClient
                            newClient.connect(
                                firebaseIdToken = token,
                                destinations = setOf("/user/queue/trip"),
                                onPayload = RideEventBus::publish,
                                onStateChanged = { connected, error ->
                                    if (!connected && error != null) Log.w("DRIVER_STOMP", error)
                                }
                            )
                        }
                    }
                }

                val location = DeviceLocationProvider.currentLocation(this)
                if (location != null) {
                    client?.sendDriverLocation(driverId, location.latitude, location.longitude)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w("DRIVER_TRACKING", "Không thể gửi vị trí tài xế", exception)
            }
            delay(5_000)
        }
    }

    private fun startAsForeground() {
        val channelId = "driver_location"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Theo dõi vị trí tài xế", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GoRide đang trực tuyến")
            .setContentText("Đang cập nhật vị trí để nhận chuyến")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        stompClient?.disconnect()
        stompClient = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 5101
        private const val EXTRA_DRIVER_ID = "driver_id"
        const val ACTION_STOP = "com.example.app_dat_xe.action.STOP_DRIVER_LOCATION"

        fun start(context: Context, driverId: Long) {
            val intent = Intent(context, DriverLocationService::class.java)
                .putExtra(EXTRA_DRIVER_ID, driverId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DriverLocationService::class.java).setAction(ACTION_STOP))
        }
    }
}
