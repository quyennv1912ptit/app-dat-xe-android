package com.example.app_dat_xe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.app_dat_xe.data.remote.RetrofitClient
import com.example.app_dat_xe.feature.auth.ui.OtpData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        "home",
        "Trang chủ",
        Icons.Default.Home
    )

    object Activity : BottomNavItem(
        "activity",
        "Hoạt động",
        Icons.AutoMirrored.Filled.List
    )

    object Message : BottomNavItem(
        "message",
        "Tin nhắn",
        Icons.AutoMirrored.Filled.Message
    )

    object Account : BottomNavItem(
        "account",
        "Tài khoản",
        Icons.Default.Person
    )
}

/** Chuyển exception thành thông báo thân thiện với người dùng. */
private fun Throwable.toUserMessage(default: String): String = when (this) {
    is IOException -> "Lỗi kết nối mạng, vui lòng kiểm tra lại đường truyền"
    else -> message?.takeIf { it.isNotBlank() } ?: default
}

/**
 * Đọc ảnh từ Uri, thu nhỏ (cạnh dài tối đa [maxSide]px) và nén JPEG
 * để tránh OutOfMemoryError và giảm dung lượng upload.
 * Gọi trong Dispatchers.IO.
 */
private fun compressImageForUpload(
    context: Context,
    uri: Uri,
    maxSide: Int = 1024,
    quality: Int = 85
): ByteArray {
    val resolver = context.contentResolver

    // Bước 1: chỉ đọc kích thước
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw Exception("Không đọc được ảnh đã chọn")

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw Exception("Tệp đã chọn không phải ảnh hợp lệ")
    }

    // Bước 2: decode với inSampleSize để tiết kiệm RAM
    var sample = 1
    while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
        sample *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: throw Exception("Không đọc được ảnh đã chọn")

    // Bước 3: scale xuống đúng kích thước tối đa
    val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        decoded
    }
    if (bitmap !== decoded) decoded.recycle()

    // Bước 4: nén JPEG
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        out.toByteArray()
    }
}

@Composable
fun MainScreen(
    fullName: String?,
    phoneNumber: String?,
    email: String?,
    role: String?,
    onLogout: () -> Unit
) {
    Log.d(
        "MAIN_SCREEN",
        "fullName=$fullName, phone=$phoneNumber, email=$email, role=$role"
    )

    val bottomNavController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Ảnh vừa chọn từ thư viện (hiển thị ngay, ưu tiên cao nhất)
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    // Avatar đã lưu trên backend
    var serverAvatarUrl by remember { mutableStateOf<String?>(null) }

    // Avatar Google/Facebook: đọc trực tiếp mỗi lần recompose,
    // không copy vào state nên luôn lấy giá trị mới nhất của OtpData.
    val providerPhotoUrl: String? = OtpData.providerPhotoUrl

    // Thứ tự ưu tiên: ảnh vừa chọn > ảnh trên server > ảnh Google/Facebook
    val avatarModel: Any? = avatarUri ?: serverAvatarUrl ?: providerPhotoUrl

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(role) {
        if (role.isNullOrBlank()) {
            avatarError = "Không xác định được vai trò tài khoản"
            return@LaunchedEffect
        }

        try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
                ?: throw Exception("Bạn chưa đăng nhập")

            val idToken = firebaseUser.getIdToken(false)
                .await()
                .token
                ?: throw Exception("Không lấy được thông tin xác thực")

            val response = RetrofitClient.api.getProfile(
                token = "Bearer $idToken",
                role = role
            )

            if (response.isSuccessful) {
                serverAvatarUrl = response.body()?.avatarUrl
                avatarError = null
            } else {
                Log.e("PROFILE_AVATAR", "Lỗi tải hồ sơ: HTTP ${response.code()}")
                // Vẫn giữ avatar Google/Facebook, nhưng báo cho người dùng biết.
                avatarError = "Không tải được ảnh đại diện (mã lỗi ${response.code()})"
            }
        } catch (e: Exception) {
            Log.e("PROFILE_AVATAR", "Lỗi tải hồ sơ", e)
            avatarError = e.toUserMessage("Lỗi tải ảnh đại diện")
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            // Người dùng huỷ chọn ảnh
        } else if (role.isNullOrBlank()) {
            // Không upload khi chưa biết vai trò, tránh upload nhầm tài khoản
            avatarError = "Không xác định được vai trò tài khoản"
        } else {
            avatarUri = uri
            avatarError = null

            coroutineScope.launch {
                try {
                    isUploadingAvatar = true

                    // Bước 1: Kiểm tra loại tệp
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType != null && !mimeType.startsWith("image/")) {
                        throw Exception("Vui lòng chọn một tệp ảnh")
                    }

                    // Bước 2: Lấy Firebase ID Token
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                        ?: throw Exception("Bạn chưa đăng nhập")

                    val idToken = firebaseUser.getIdToken(false)
                        .await()
                        .token
                        ?: throw Exception("Không lấy được thông tin xác thực")

                    // Bước 3: Thu nhỏ + nén ảnh (chạy ngoài main thread)
                    val imageBytes = withContext(Dispatchers.IO) {
                        compressImageForUpload(context, uri)
                    }

                    // Bước 4: Tạo MultipartBody.Part (luôn là JPEG sau khi nén)
                    val requestBody = imageBytes.toRequestBody(
                        "image/jpeg".toMediaTypeOrNull()
                    )
                    val imagePart = MultipartBody.Part.createFormData(
                        name = "file",
                        filename = "avatar.jpg",
                        body = requestBody
                    )

                    // Bước 5: Upload lên backend
                    val response = RetrofitClient.api.uploadAvatar(
                        token = "Bearer $idToken",
                        role = role,
                        file = imagePart
                    )

                    if (response.isSuccessful) {
                        val updatedProfile = response.body()
                            ?: throw Exception("Backend không trả về thông tin hồ sơ")

                        serverAvatarUrl = updatedProfile.avatarUrl
                        avatarUri = null

                        Log.d(
                            "AVATAR_UPLOAD",
                            "Upload thành công: ${updatedProfile.avatarUrl}"
                        )
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(
                            "AVATAR_UPLOAD",
                            "Upload thất bại: ${response.code()} - $errorBody"
                        )
                        throw Exception("Upload ảnh thất bại (mã lỗi ${response.code()})")
                    }
                } catch (e: Exception) {
                    Log.e("AVATAR_UPLOAD", "Lỗi upload ảnh", e)
                    // Quay về ảnh cũ khi upload lỗi
                    avatarUri = null
                    avatarError = e.toUserMessage("Không thể cập nhật ảnh đại diện")
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Activity,
        BottomNavItem.Message,
        BottomNavItem.Account
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // TRANG CHỦ
            composable(BottomNavItem.Home.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Giao diện Trang chủ")
                }
            }

            // HOẠT ĐỘNG
            composable(BottomNavItem.Activity.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Giao diện Lịch sử chuyến đi")
                }
            }

            // TIN NHẮN
            composable(BottomNavItem.Message.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Giao diện Tin nhắn & Thông báo")
                }
            }

            // TÀI KHOẢN
            composable(BottomNavItem.Account.route) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {

                    Text(
                        text = "Tài khoản",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ẢNH ĐẠI DIỆN
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (avatarModel != null) {
                                AsyncImage(
                                    model = avatarModel,
                                    contentDescription = "Ảnh đại diện",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Ảnh mặc định",
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                enabled = !isUploadingAvatar && !role.isNullOrBlank()
                            ) {
                                Text("Đổi ảnh đại diện")
                            }

                            if (isUploadingAvatar) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CircularProgressIndicator()
                            }

                            avatarError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            // HỌ VÀ TÊN
                            Text(
                                text = "Họ và tên",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = fullName ?: "Chưa có tên")

                            Spacer(modifier = Modifier.height(20.dp))

                            // SỐ ĐIỆN THOẠI
                            Text(
                                text = "Số điện thoại",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = phoneNumber ?: "Chưa có số điện thoại")

                            Spacer(modifier = Modifier.height(20.dp))

                            // EMAIL
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (email.isNullOrBlank()) {
                                    "Chưa có email"
                                } else {
                                    email
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // VAI TRÒ
                            Text(
                                text = "Vai trò",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (role) {
                                    "DRIVER" -> "Lái xe"
                                    "CUSTOMER" -> "Khách hàng"
                                    else -> "Chưa xác định"
                                }
                            )
                        }
                    }

                    // Nút Đăng xuất nằm NGOÀI Card
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đăng xuất")
                    }
                }
            }
        }
    }
}