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

        SessionStore.role?.let { requestBuilder.header("X-Role", it) }

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
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}

class FirebaseTokenAuthenticator : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: Response): okhttp3.Request? {
        if (responseCount(response) >= 2) return null
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val currentHeader = response.request.header("Authorization")
        val refreshedToken = runBlocking {
            try {
                user.getIdToken(true).await().token
            } catch (_: Exception) {
                null
            }
        }?.takeIf(String::isNotBlank) ?: return null
        val authorization = "Bearer $refreshedToken"
        if (authorization == currentHeader) return null
        return response.request.newBuilder().header("Authorization", authorization).build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
