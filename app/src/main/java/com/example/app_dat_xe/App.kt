package com.example.app_dat_xe

import android.app.Application
import com.example.app_dat_xe.data.remote.SessionStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionStore.initialize(this)
    }
}
