package com.example.app_dat_xe.data.remote

import android.content.Context

object SessionStore {
    @Volatile
    private var savedRole: String? = null

    val role: String?
        get() = savedRole

    fun initialize(context: Context) {
        savedRole = context.getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE)
            .getString("role", null)
    }

    fun setRole(context: Context, role: String?) {
        savedRole = role
        context.getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE)
            .edit()
            .putString("role", role)
            .apply()
    }

    fun clear() {
        savedRole = null
    }
}
