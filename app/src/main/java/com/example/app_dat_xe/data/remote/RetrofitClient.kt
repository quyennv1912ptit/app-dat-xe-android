package com.example.app_dat_xe.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {

    val BASE_URL: String = com.example.app_dat_xe.BuildConfig.BASE_URL
    val STOMP_URL: String = BASE_URL.removeSuffix("/")
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/ws"

    val logging = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        redactHeader("X-Role")
        level = if (com.example.app_dat_xe.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(logging)
        .authenticator(FirebaseTokenAuthenticator())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val webSocketClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ApiEnvelopeConverterFactory())
            .build()
            .create(ApiService::class.java)
    }
}
