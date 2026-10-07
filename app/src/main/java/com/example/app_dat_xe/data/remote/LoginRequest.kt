package com.example.app_dat_xe.data.remote

data class LoginRequest(
    val idToken: String,
    val role: String? = null
)