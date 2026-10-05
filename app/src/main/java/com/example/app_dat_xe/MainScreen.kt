package com.example.app_dat_xe

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.app_dat_xe.data.remote.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody


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
        Icons.Default.List
    )
    object Message : BottomNavItem(
        "message",
        "Tin nhắn",
        Icons.Default.Message
    )
    object Account : BottomNavItem(
        "account",
        "Tài khoản",
        Icons.Default.Person
    )
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

    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    // Hiển thị avatar Google/Facebook ngay từ URL đã lấy khi đăng nhập
    var avatarUrl by remember {
        mutableStateOf(
            com.example.app_dat_xe.feature.auth.ui.OtpData.providerPhotoUrl
        )
    }

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(role) {
        if (role.isNullOrBlank()) {
            avatarError = "Không xác định được vai trò tài khoản"
            return@LaunchedEffect
        }

        try {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
                ?: throw Exception("Bạn chưa đăng nhập Firebase")

            val idToken = firebaseUser.getIdToken(false)
                .await()
                .token
                ?: throw Exception("Không lấy được Firebase ID Token")

            val response = RetrofitClient.api.getProfile(
                token = "Bearer $idToken",
                role = role
            )

            if (response.isSuccessful) {
                val serverAvatar = response.body()?.avatarUrl
                // Ưu tiên avatar đã lưu trên backend,
                // nếu backend chưa có thì giữ ảnh Google/Facebook.
                avatarUrl = serverAvatar
                    ?: com.example.app_dat_xe.feature.auth.ui.OtpData.providerPhotoUrl
            } else {
                Log.e(
                    "PROFILE_AVATAR",
                    "Lỗi tải hồ sơ: HTTP ${response.code()}"
                )
                // Không xóa avatar Google/Facebook khi API lỗi.
                avatarError = null
            }
        } catch (e: Exception) {
            avatarError = e.message ?: "Lỗi tải ảnh đại diện"
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri
            avatarError = null

            coroutineScope.launch {
                try {
                    isUploadingAvatar = true

                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                        ?: throw Exception("Bạn chưa đăng nhập Firebase")

                    // Bước 1: Lấy Firebase ID Token
                    val idToken = firebaseUser.getIdToken(false)
                        .await()
                        .token
                        ?: throw Exception("Không lấy được Firebase ID Token")

                    // Bước 2: Đọc ảnh từ URI
                    val contentResolver = context.contentResolver

                    val mimeType = contentResolver.getType(uri)
                        ?: "image/jpeg"

                    val imageBytes = contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: throw Exception("Không đọc được ảnh đã chọn")

                    // Bước 3: Tạo MultipartBody.Part
                    val requestBody = imageBytes.toRequestBody(
                        mimeType.toMediaTypeOrNull()
                    )

                    val imagePart = MultipartBody.Part.createFormData(
                        name = "file",
                        filename = "avatar.${mimeType.substringAfter("/", "jpg")}",
                        body = requestBody
                    )

                    // Bước 4: Upload ảnh lên backend
                    val response = RetrofitClient.api.uploadAvatar(
                        token = "Bearer $idToken",
                        role = role ?: "CUSTOMER",
                        file = imagePart
                    )

                    if (response.isSuccessful) {
                        val updatedProfile = response.body()
                            ?: throw Exception("Backend không trả về thông tin hồ sơ")

                        avatarUrl = updatedProfile.avatarUrl
                        avatarUri = null

                        Log.d(
                            "AVATAR_UPLOAD",
                            "Upload thành công: ${updatedProfile.avatarUrl}"
                        )
                    } else {
                        val errorBody = response.errorBody()?.string()

                        throw Exception(
                            "Upload ảnh thất bại: ${response.code()} - $errorBody"
                        )
                    }

                } catch (e: Exception) {
                    avatarError = e.message ?: "Không thể cập nhật ảnh đại diện"
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

                val navBackStackEntry by
                bottomNavController.currentBackStackEntryAsState()

                val currentRoute =
                    navBackStackEntry?.destination?.route

                items.forEach { item ->

                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.title
                            )
                        },

                        label = {
                            Text(item.title)
                        },

                        selected = currentRoute == item.route,

                        onClick = {

                            bottomNavController.navigate(item.route) {

                                popUpTo(
                                    bottomNavController
                                        .graph
                                        .startDestinationId
                                ) {
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
                            if (avatarUrl != null || avatarUri != null) {
                                AsyncImage(
                                    model = avatarUrl ?: avatarUri,
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
                                onClick = {
                                    imagePicker.launch("image/*")
                                },
                                enabled = !isUploadingAvatar
                            ) {
                                Text("Đổi ảnh đại diện")
                            }

                            if (isUploadingAvatar) {
                                CircularProgressIndicator()
                            }

                            avatarError?.let { error ->
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

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = fullName ?: "Chưa có tên"
                            )

                            Spacer(
                                modifier = Modifier.height(20.dp)
                            )

                            // SỐ ĐIỆN THOẠI
                            Text(
                                text = "Số điện thoại",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = phoneNumber ?: "Chưa có số điện thoại"
                            )

                            Spacer(
                                modifier = Modifier.height(20.dp)
                            )

                            // EMAIL
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = if (email.isNullOrBlank()) {
                                    "Chưa có email"
                                } else {
                                    email
                                }
                            )

                            Spacer(
                                modifier = Modifier.height(20.dp)
                            )

                            // VAI TRÒ
                            Text(
                                text = "Vai trò",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = when (role) {
                                    "DRIVER" -> "Lái xe"
                                    "CUSTOMER" -> "Khách hàng"
                                    else -> "Chưa xác định"
                                }
                            )
                        }
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
}