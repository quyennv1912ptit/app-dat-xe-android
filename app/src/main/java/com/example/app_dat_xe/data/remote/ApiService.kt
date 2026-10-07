package com.example.app_dat_xe.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @POST("api/auth/facebook")
    suspend fun loginWithFacebook(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @POST("api/auth/link-phone")
    suspend fun linkPhone(
        @Body request: LinkPhoneRequest
    ): Response<LoginResponse>
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
    @POST("api/auth/google")
    suspend fun loginWithGoogle(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @GET("api/auth/test")
    suspend fun testBackend(): Response<String>
    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Query("role") role: String
    ): Response<ProfileResponse>

    @PUT("api/profile/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Query("role") role: String,
        @Body request: AvatarRequest
    ): Response<ProfileResponse>

    @Multipart
    @POST("api/profile/avatar/upload")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Query("role") role: String,
        @Part file: MultipartBody.Part
    ): Response<ProfileResponse>

}