package com.example.app_dat_xe.data.remote

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object CloudinaryUploader {
    private val client = OkHttpClient()

    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val cloudName = com.example.app_dat_xe.BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        val uploadPreset = com.example.app_dat_xe.BuildConfig.CLOUDINARY_UNSIGNED_UPLOAD_PRESET.trim()
        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            throw IllegalStateException("Hãy cấu hình CLOUDINARY_CLOUD_NAME và CLOUDINARY_UNSIGNED_UPLOAD_PRESET trong local.properties.")
        }

        val imageBytes = ImageUploadCompressor.compress(context, uri)
        val mimeType = "image/jpeg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart("file", "handover.jpg", imageBytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Cloudinary upload lỗi HTTP ${response.code}")
            JSONObject(responseText).optString("secure_url").takeIf { it.isNotBlank() }
                ?: throw IOException("Cloudinary không trả về secure_url")
        }
    }
}
