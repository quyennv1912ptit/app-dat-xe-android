package com.example.app_dat_xe.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.app_dat_xe.R
import com.example.app_dat_xe.MainActivity
import com.example.app_dat_xe.data.remote.DeviceTokenRegistrar
import com.example.app_dat_xe.tracking.RideEventBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonParser
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
        val raw = message.data["payload"] ?: return
        val root = try {
            JsonParser.parseString(raw).asJsonObject
        } catch (_: Exception) {
            return
        }
        val type = root.get("type")?.takeUnless { it.isJsonNull }?.asString ?: return
        if (!RideEventBus.publish("/user/queue/trip", raw)) return
        val notification = when (type) {
            "RIDE_OFFER" -> "Có chuyến mới" to "Chạm để xem và nhận chuyến"
            "RIDE_ASSIGNED" -> "Đã có tài xế nhận chuyến" to "Chạm để xem tài xế"
            "RIDE_NO_DRIVER" -> "Chưa tìm được tài xế" to "Bạn có thể thử đặt lại"
            "RIDE_CANCELLED" -> "Chuyến đã bị hủy" to ""
            "RIDE_COMPLETED" -> "Chuyến đã hoàn tất" to ""
            else -> return
        }
        val id = root.get("messageId")?.takeUnless { it.isJsonNull }?.asString?.hashCode()
            ?: message.messageId?.hashCode()
            ?: System.currentTimeMillis().toInt()
        showNotification(notification.first, notification.second, id)
    }

    private fun showNotification(title: String, body: String, id: Int) {
        val channelId = "ride_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Cập nhật chuyến đi", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            manager.notify(id, notification)
        } catch (exception: SecurityException) {
            Log.i("PUSH_NOTIFICATION", "Thông báo chưa được cấp quyền hiển thị")
        }
    }
}
