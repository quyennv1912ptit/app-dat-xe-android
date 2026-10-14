package com.example.app_dat_xe.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app_dat_xe.R
import com.example.app_dat_xe.data.remote.DeviceTokenRegistrar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RideFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            serviceScope.launch {
                DeviceTokenRegistrar.registerToken(applicationContext, token)
            }
        } else {
            getSharedPreferences("app_dat_xe_push", Context.MODE_PRIVATE)
                .edit().putString("pending_fcm_token", token).apply()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "GoRide"
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body, message.messageId?.hashCode() ?: System.currentTimeMillis().toInt())
    }

    private fun showNotification(title: String, body: String, id: Int) {
        val channelId = "ride_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Cập nhật chuyến đi", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            manager.notify(id, notification)
        } catch (exception: SecurityException) {
            Log.i("PUSH_NOTIFICATION", "Thông báo chưa được cấp quyền hiển thị")
        }
    }
}
