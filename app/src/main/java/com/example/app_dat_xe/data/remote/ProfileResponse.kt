package com.example.app_dat_xe.data.remote

data class ProfileResponse(
    val id: Long?,
    val uid: String?,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val avatarUrl: String?,
    val role: String?
)