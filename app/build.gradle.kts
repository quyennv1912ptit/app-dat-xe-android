import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val projectLocalProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val signingProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun quotedBuildConfig(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseBaseUrl = projectLocalProperties.getProperty("GORIDE_BASE_URL_RELEASE", "https://api.example.invalid/")
require(releaseBaseUrl.startsWith("https://")) { "GORIDE_BASE_URL_RELEASE must use HTTPS." }
val debugBaseUrl = projectLocalProperties.getProperty("GORIDE_BASE_URL_DEBUG", "http://192.168.1.101:8080/")
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !signingProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.example.app_dat_xe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.app_dat_xe"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "BASE_URL", quotedBuildConfig(debugBaseUrl))
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", quotedBuildConfig(projectLocalProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")))
            buildConfigField("String", "CLOUDINARY_UNSIGNED_UPLOAD_PRESET", quotedBuildConfig(projectLocalProperties.getProperty("CLOUDINARY_UNSIGNED_UPLOAD_PRESET", "")))
            manifestPlaceholders["cleartextEnabled"] = "true"
            manifestPlaceholders["facebookClientToken"] = projectLocalProperties.getProperty("FACEBOOK_CLIENT_TOKEN", "")
        }
        release {
            buildConfigField("String", "BASE_URL", quotedBuildConfig(releaseBaseUrl))
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", quotedBuildConfig(projectLocalProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")))
            buildConfigField("String", "CLOUDINARY_UNSIGNED_UPLOAD_PRESET", quotedBuildConfig(projectLocalProperties.getProperty("CLOUDINARY_UNSIGNED_UPLOAD_PRESET", "")))
            manifestPlaceholders["cleartextEnabled"] = "false"
            manifestPlaceholders["facebookClientToken"] = projectLocalProperties.getProperty("FACEBOOK_CLIENT_TOKEN", "")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.mapbox.maps:android:11.30.1")
    implementation("com.mapbox.extension:maps-compose:11.30.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.facebook.android:facebook-login:18.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
