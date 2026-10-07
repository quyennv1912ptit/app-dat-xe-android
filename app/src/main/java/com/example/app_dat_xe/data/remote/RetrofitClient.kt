package com.example.app_dat_xe.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://192.168.1.101:8080/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())

        .addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request()

            Log.d(
                "API_DEBUG",
                "Gửi request: ${request.method} ${request.url}"
            )

            try {
                val response = chain.proceed(request)

                Log.d(
                    "API_DEBUG",
                    "Response: ${response.code} ${response.message}"
                )

                response
            } catch (e: Exception) {
                Log.e(
                    "API_DEBUG",
                    "REQUEST ERROR: ${e.javaClass.name}: ${e.message}",
                    e
                )
                throw e
            }
        }
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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