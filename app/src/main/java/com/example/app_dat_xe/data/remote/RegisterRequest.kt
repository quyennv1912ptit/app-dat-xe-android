package com.example.app_dat_xe.data.remote

data class RegisterRequest(
    val idToken: String,
    val phoneNumber: String,
    val fullName: String,
    val email: String?,
    val role: String,
    val provider: String
)