package com.example.app_dat_xe.data.remote

import android.content.Context
import android.net.Uri
import com.example.app_dat_xe.R
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
        val cloudName = context.getString(R.string.cloudinary_cloud_name).trim()
        val uploadPreset = context.getString(R.string.cloudinary_unsigned_upload_preset).trim()
        if (cloudName.startsWith("CONFIGURE_") || uploadPreset.startsWith("CONFIGURE_")) {
            throw IllegalStateException("Hãy cấu hình Cloudinary cloud name và unsigned upload preset trong strings.xml.")
        }

        val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Không đọc được ảnh đã chọn")
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
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
