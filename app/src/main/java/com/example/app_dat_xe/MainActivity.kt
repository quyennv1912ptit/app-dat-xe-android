package com.example.app_dat_xe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import android.content.Intent
import android.util.Log
import com.facebook.FacebookSdk
import com.example.app_dat_xe.feature.auth.ui.FacebookCallbackManagerHolder
import androidx.compose.ui.unit.dp
import com.example.app_dat_xe.feature.auth.ui.LoginScreen
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FacebookSdk.sdkInitialize(applicationContext)
        setContent {

            Surface (
                modifier = Modifier.fillMaxSize()
            ) {
                AppNavigation()
            }

        }
    }
}