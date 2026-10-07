package com.example.app_dat_xe.data.remote

data class LinkPhoneRequest(
    val providerIdToken: String,
    val phoneIdToken: String,
    val phoneNumber: String,
    val role: String
)