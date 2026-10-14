package com.example.app_dat_xe.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.UUID

object DeviceTokenRegistrar {
    private const val PREFS = "app_dat_xe_push"
    private const val INSTALLATION_ID = "installation_id"
    private const val PENDING_TOKEN = "pending_fcm_token"

    suspend fun registerCurrentToken(context: Context) {
        val token = FirebaseMessaging.getInstance().token.await()
        registerToken(context, token)
    }

    suspend fun registerToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installationId = prefs.getString(INSTALLATION_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(INSTALLATION_ID, it).apply()
        }
        prefs.edit().putString(PENDING_TOKEN, token).apply()

        if (FirebaseAuth.getInstance().currentUser == null) return

        try {
            val response = RetrofitClient.api.registerDeviceToken(
                DeviceTokenRequest(
                    installationId = installationId,
                    fcmToken = token,
                    platform = "ANDROID"
                )
            )
            if (response.isSuccessful) {
                prefs.edit().remove(PENDING_TOKEN).apply()
            } else {
                Log.w("PUSH_REGISTRATION", "Đăng ký token thất bại: HTTP ${response.code()}")
            }
        } catch (exception: Exception) {
            Log.w("PUSH_REGISTRATION", "Không thể đăng ký token thiết bị", exception)
        }
    }

    suspend fun unregisterCurrentInstallation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installationId = prefs.getString(INSTALLATION_ID, null) ?: return
        if (FirebaseAuth.getInstance().currentUser == null) return
        val response = RetrofitClient.api.unregisterDeviceToken(installationId)
        if (response.isSuccessful) {
            prefs.edit().remove(PENDING_TOKEN).apply()
        } else {
            Log.w("PUSH_REGISTRATION", "Hủy đăng ký token thất bại: HTTP ${response.code()}")
        }
    }
}
