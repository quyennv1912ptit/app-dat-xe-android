package com.example.app_dat_xe.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.app_dat_xe.data.remote.LoginResponse
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.example.app_dat_xe.data.remote.RegisterRequest
import com.example.app_dat_xe.data.remote.ImageUploadCompressor
import com.google.firebase.auth.FirebaseAuth
import com.example.app_dat_xe.data.remote.RetrofitClient
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun SignUpScreen(role: String, onSignUpSuccess: (LoginResponse) -> Unit) {
    var fullName by remember {
        mutableStateOf(OtpData.facebookName ?: "")
    }
    var email by remember { mutableStateOf("") }
    var isAgreed by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) avatarUri = uri
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text(text = "Điều khoản & Chính sách", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Dịch vụ đặt xe\nChúng tôi cung cấp nền tảng kết nối giữa hành khách và tài xế...\n\n2. Quyền riêng tư\nDữ liệu định vị của bạn chỉ được sử dụng trong thời gian diễn ra chuyến đi nhằm đảm bảo an toàn.\n\n3. Trách nhiệm\nHành khách cam kết thanh toán đủ cước phí hiển thị trên ứng dụng.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hoàn thiện hồ sơ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { photoPicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Ảnh đại diện đã chọn",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Chọn ảnh đại diện",
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }
        }
        Text("Chạm để chọn ảnh đại diện", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(text = "Họ và tên") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Địa chỉ email (Không bắt buộc)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        val annotatedTermsString = buildAnnotatedString {
            append("Tôi đồng ý với ")
            pushStringAnnotation(tag = "TERMS", annotation = "terms_clicked")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("điều khoản dịch vụ và chính sách bảo mật")
            }
            pop()
            append(".")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isAgreed,
                onCheckedChange = { isAgreed = it }
            )

            Text(
                text = annotatedTermsString,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                modifier = Modifier.padding(start = 4.dp).clickable { showTermsDialog = true }
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isSubmitting) return@Button

                val idToken = OtpData.phoneIdToken
                val phoneNumber = OtpData.phoneNumber

                if (idToken.isNullOrBlank() || phoneNumber.isNullOrBlank()) {
                    Toast.makeText(
                        context,
                        "Chưa xác thực số điện thoại",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                val request = RegisterRequest(
                    idToken = idToken,
                    phoneNumber = phoneNumber,
                    fullName = fullName,
                    email = email.ifBlank { null },
                    role = role,
                    provider = "PHONE"
                )

                scope.launch {
                    isSubmitting = true
                    try {
                        val response = RetrofitClient.api.register(request)

                        if (response.isSuccessful) {

                            val result = response.body()

                            if (result != null) {
                                val selectedAvatar = avatarUri
                                var avatarUploadFailed = false
                                if (selectedAvatar != null) {
                                    try {
                                        val mimeType = context.contentResolver.getType(selectedAvatar)
                                        require(mimeType == null || mimeType.startsWith("image/")) {
                                            "Vui lòng chọn một tệp ảnh"
                                        }
                                        val firebaseUser = auth.currentUser
                                            ?: throw IllegalStateException("Không lấy được phiên đăng nhập để tải ảnh")
                                        val freshToken = firebaseUser.getIdToken(false).await().token
                                            ?: throw IllegalStateException("Không lấy được thông tin xác thực")
                                        val imageBytes = withContext(Dispatchers.IO) {
                                            ImageUploadCompressor.compress(context, selectedAvatar)
                                        }
                                        val imagePart = MultipartBody.Part.createFormData(
                                            name = "file",
                                            filename = "avatar.jpg",
                                            body = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                        )
                                        val uploadResponse = RetrofitClient.api.uploadAvatar(
                                            token = "Bearer $freshToken",
                                            role = role,
                                            file = imagePart
                                        )
                                        avatarUploadFailed = !uploadResponse.isSuccessful
                                    } catch (_: Exception) {
                                        avatarUploadFailed = true
                                    }
                                }

                                Toast.makeText(
                                    context,
                                    if (avatarUploadFailed) "Đăng ký thành công, nhưng chưa tải được ảnh đại diện. Bạn có thể thử lại trong hồ sơ."
                                    else "Đăng ký thành công",
                                    Toast.LENGTH_LONG
                                ).show()

                                OtpData.phoneIdToken = null
                                OtpData.phoneNumber = null
                                OtpData.verificationId = null

                                onSignUpSuccess(result)
                            }

                        } else {

                            val error = response.errorBody()?.string()

                            Toast.makeText(
                                context,
                                error ?: "Đăng ký thất bại",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Lỗi kết nối: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = fullName.isNotBlank() && isAgreed && !isSubmitting
        ) {
            Text(if (isSubmitting) "ĐANG TẠO TÀI KHOẢN…" else "Bắt đầu sử dụng", style = MaterialTheme.typography.titleMedium)
        }
    }
}
