package com.example.app_dat_xe.data.remote

data class LoginResponse(
    val id: Long?,
    val uid: String?,
    val email: String?,
    val phoneNumber: String?,
    val fullName: String?,
    val role: String?
)