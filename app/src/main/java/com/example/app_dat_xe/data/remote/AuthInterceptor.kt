package com.example.app_dat_xe.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        if (originalRequest.header("Authorization") == null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val token = runBlocking {
                    try {
                        currentUser.getIdToken(false).await().token
                    } catch (e: Exception) {
                        null
                    }
                }

                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}