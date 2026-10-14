package com.example.app_dat_xe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.app_dat_xe.data.remote.RetrofitClient
import com.example.app_dat_xe.data.remote.CancelRideRequest
import com.example.app_dat_xe.data.remote.CreateRideRequest
import com.example.app_dat_xe.data.remote.DriverAvailabilityRequest
import com.example.app_dat_xe.data.remote.DeviceTokenRegistrar
import com.example.app_dat_xe.data.remote.EstimateRideRequest
import com.example.app_dat_xe.data.remote.ConfirmHandoverRequest
import com.example.app_dat_xe.data.remote.CreateHandoverReportRequest
import com.example.app_dat_xe.data.remote.HandoverPhotoRequest
import com.example.app_dat_xe.data.remote.RideProgressRequest
import com.example.app_dat_xe.data.remote.CloudinaryUploader
import com.example.app_dat_xe.data.remote.RideStompClient
import com.example.app_dat_xe.feature.auth.ui.OtpData
import com.example.app_dat_xe.feature.ride.ui.CustomerHomeScreen
import com.example.app_dat_xe.feature.ride.ui.DriverActiveTripScreen
import com.example.app_dat_xe.feature.ride.ui.DriverHomeScreen
import com.example.app_dat_xe.feature.ride.ui.DriverWalletScreen
import com.example.app_dat_xe.feature.ride.ui.FindingDriverScreen
import com.example.app_dat_xe.feature.ride.ui.RideHandoverScreen
import com.example.app_dat_xe.feature.ride.ui.IncomingTripScreen
import com.example.app_dat_xe.feature.ride.ui.NotificationScreen
import com.example.app_dat_xe.feature.ride.ui.RatingScreen
import com.example.app_dat_xe.feature.ride.ui.RideHistoryScreen
import com.example.app_dat_xe.feature.ride.ui.SelectLocationScreen
import com.example.app_dat_xe.feature.ride.ui.TripOptionsScreen
import com.example.app_dat_xe.feature.ride.ui.TripTrackingScreen
import com.example.app_dat_xe.feature.ride.ui.RidePlace
import com.example.app_dat_xe.data.remote.RideOfferPayload
import com.example.app_dat_xe.tracking.DriverLocationService
import com.example.app_dat_xe.tracking.RideEventBus
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        "Thông báo",
        Icons.AutoMirrored.Filled.Message
    )

    object Account : BottomNavItem(
        "account",
        "Tài khoản",
        Icons.Default.Person
    )

    object Wallet : BottomNavItem(
        "wallet",
        "Ví",
        Icons.Default.AccountBalanceWallet
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

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw Exception("Tệp không hợp lệ")

    var rotationDegrees = 0f
    try {
        resolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    } catch (e: Exception) {
        Log.e("EXIF_ERROR", "Không đọc được góc xoay", e)
    }

    var sample = 1
    while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
        sample *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: throw Exception("Không đọc được nội dung ảnh")

    val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
    var bitmap = if (scale < 1f) {
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== decoded) decoded.recycle()
        scaled
    } else {
        decoded
    }

    if (rotationDegrees != 0f) {
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotatedBitmap !== bitmap) bitmap.recycle()
        bitmap = rotatedBitmap
    }

    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        out.toByteArray()
    }
}

@Composable
fun MainScreen(
    userId: Long?,
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
    val prefs = remember(context) {
        context.getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE)
    }

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
        if (role == "DRIVER") BottomNavItem.Wallet else BottomNavItem.Message,
        BottomNavItem.Account
    )
    val isDriver = role == "DRIVER"
    var isDriverOnline by remember { mutableStateOf(prefs.getBoolean("driver_online", false)) }
    var pickupPlace by remember { mutableStateOf(RidePlace("Vị trí hiện tại")) }
    var destinationPlace by remember { mutableStateOf(RidePlace("")) }
    var estimate by remember { mutableStateOf<com.example.app_dat_xe.data.remote.EstimateRideResponse?>(null) }
    var estimatedVehicleId by remember { mutableStateOf<Long?>(null) }
    var estimateError by remember { mutableStateOf<String?>(null) }
    var bookingError by remember { mutableStateOf<String?>(null) }
    var isEstimating by remember { mutableStateOf(false) }
    var selectedRideId by remember { mutableStateOf<Long?>(null) }
    var rideStatus by remember { mutableStateOf("SEARCHING") }
    var finalFare by remember { mutableStateOf<Long?>(null) }
    var driverName by remember { mutableStateOf<String?>(null) }
    var driverPhone by remember { mutableStateOf<String?>(null) }
    var driverRating by remember { mutableStateOf<Double?>(null) }
    var driverLocationText by remember { mutableStateOf<String?>(null) }
    var currentOffer by remember { mutableStateOf<RideOfferPayload?>(null) }
    var handledOfferId by remember { mutableStateOf<Long?>(null) }
    var isAcceptingOffer by remember { mutableStateOf(false) }
    var offerError by remember { mutableStateOf<String?>(null) }
    var activeTripStep by remember { mutableIntStateOf(0) }
    var isUpdatingTrip by remember { mutableStateOf(false) }
    var tripProgressError by remember { mutableStateOf<String?>(null) }
    var handoverPhase by remember { mutableStateOf("PICKUP") }
    var isSubmittingHandover by remember { mutableStateOf(false) }
    var handoverError by remember { mutableStateOf<String?>(null) }
    var isAvailabilityBusy by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var customerStompClient by remember { mutableStateOf<RideStompClient?>(null) }
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun showRideMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun backendError(body: String?, fallback: String): String {
        return try {
            val root = JsonParser.parseString(body ?: "{}").asJsonObject
            root.getAsJsonObject("error")?.get("message")?.asString ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun estimateRide(vehicleId: Long) {
        val pickup = pickupPlace
        val destination = destinationPlace
        val pickupLat = pickup.latitude
        val pickupLng = pickup.longitude
        val dropoffLat = destination.latitude
        val dropoffLng = destination.longitude
        if (pickupLat == null || pickupLng == null || dropoffLat == null || dropoffLng == null) {
            estimateError = "Hãy chọn điểm đón và điểm đến có tọa độ GPS."
            return
        }
        coroutineScope.launch {
            isEstimating = true
            estimateError = null
            estimate = null
            estimatedVehicleId = null
            try {
                val response = RetrofitClient.api.estimateRide(
                    EstimateRideRequest(
                        vehicleId = vehicleId,
                        pickupAddress = pickup.address,
                        pickupLat = pickupLat,
                        pickupLng = pickupLng,
                        dropoffAddress = destination.address,
                        dropoffLat = dropoffLat,
                        dropoffLng = dropoffLng
                    )
                )
                if (response.isSuccessful) {
                    estimate = response.body()
                    estimatedVehicleId = vehicleId
                    prefs.edit().putString("customer_vehicle_id", vehicleId.toString()).apply()
                } else {
                    estimateError = backendError(response.errorBody()?.string(), "Không tính được giá chuyến đi (${response.code()}).")
                }
            } catch (exception: Exception) {
                estimateError = exception.message ?: "Không kết nối được máy chủ để tính giá."
            } finally {
                isEstimating = false
            }
        }
    }

    fun connectCustomerRideUpdates(rideId: Long) {
        coroutineScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                    ?: throw IllegalStateException("Không lấy được token đăng nhập")
                customerStompClient?.disconnect()
                val client = RideStompClient()
                customerStompClient = client
                client.connect(
                    firebaseIdToken = token,
                    destinations = setOf("/user/queue/trip", "/user/queue/rides/$rideId/location"),
                    onPayload = { destination, rawPayload ->
                        coroutineScope.launch {
                            try {
                                val root = JsonParser.parseString(rawPayload).asJsonObject
                                if (destination.endsWith("/location")) {
                                    val location = root.getAsJsonObject("location") ?: root
                                    val lat = location.get("lat")?.asDouble
                                    val lng = location.get("lng")?.asDouble
                                    if (lat != null && lng != null) driverLocationText = "${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
                                } else {
                                    val type = root.get("type")?.asString.orEmpty()
                                    val payload = root.getAsJsonObject("payload") ?: return@launch
                                    val eventRideId = payload.get("rideId")?.asLong
                                    if (eventRideId != null && eventRideId != rideId) return@launch
                                    when (type) {
                                        "RIDE_ASSIGNED" -> {
                                            rideStatus = "DRIVER_ASSIGNED"
                                            val driver = payload.getAsJsonObject("driver")
                                            driverName = driver?.get("name")?.takeUnless { it.isJsonNull }?.asString
                                            driverPhone = driver?.get("phone")?.takeUnless { it.isJsonNull }?.asString
                                            driverRating = driver?.get("rating")?.takeUnless { it.isJsonNull }?.asDouble
                                            if (bottomNavController.currentBackStackEntry?.destination?.route == "customer/finding-driver") {
                                                bottomNavController.navigate("customer/tracking")
                                            }
                                        }
                                        "RIDE_UPDATED" -> {
                                            rideStatus = payload.get("status")?.asString ?: rideStatus
                                            if (rideStatus == "COMPLETED" && bottomNavController.currentBackStackEntry?.destination?.route != "customer/rating") {
                                                finalFare = payload.get("finalFare")?.asLong ?: finalFare
                                                bottomNavController.navigate("customer/rating")
                                            }
                                        }
                                        "RIDE_COMPLETED" -> {
                                            finalFare = payload.get("finalFare")?.asLong ?: finalFare
                                            rideStatus = "COMPLETED"
                                            if (bottomNavController.currentBackStackEntry?.destination?.route != "customer/rating") {
                                                bottomNavController.navigate("customer/rating")
                                            }
                                        }
                                        "RIDE_NO_DRIVER", "RIDE_CANCELLED" -> {
                                            bookingError = "Chuyến đi đã bị hủy hoặc không tìm thấy tài xế."
                                            showRideMessage(bookingError!!)
                                        }
                                    }
                                }
                            } catch (exception: Exception) {
                                Log.w("RIDE_UPDATES", "Không đọc được WebSocket payload", exception)
                            }
                        }
                    },
                    onStateChanged = { connected, error ->
                        if (!connected && error != null) Log.w("RIDE_UPDATES", error)
                    }
                )
            } catch (exception: Exception) {
                showRideMessage(exception.message ?: "Không kết nối được cập nhật chuyến đi.")
            }
        }
    }

    fun createRide(vehicleId: Long, paymentMethod: String) {
        if (estimate == null || estimatedVehicleId != vehicleId) {
            bookingError = "Hãy tính lại giá cho xe đang chọn trước khi đặt."
            return
        }
        coroutineScope.launch {
            bookingError = null
            try {
                val response = RetrofitClient.api.createRide(
                    CreateRideRequest(
                        vehicleId = vehicleId,
                        pickupAddress = pickupPlace.address,
                        pickupLat = pickupPlace.latitude ?: return@launch,
                        pickupLng = pickupPlace.longitude ?: return@launch,
                        dropoffAddress = destinationPlace.address,
                        dropoffLat = destinationPlace.latitude ?: return@launch,
                        dropoffLng = destinationPlace.longitude ?: return@launch,
                        paymentMethod = paymentMethod
                    )
                )
                if (!response.isSuccessful) {
                    bookingError = backendError(response.errorBody()?.string(), "Không đặt được chuyến (${response.code()}).")
                    return@launch
                }
                val createdRide = response.body() ?: throw IllegalStateException("Máy chủ chưa trả về mã chuyến")
                selectedRideId = createdRide.id
                rideStatus = createdRide.status
                finalFare = createdRide.estimatedFare
                connectCustomerRideUpdates(createdRide.id)
                bottomNavController.navigate("customer/finding-driver")
            } catch (exception: Exception) {
                bookingError = exception.message ?: "Không kết nối được máy chủ để đặt chuyến."
            }
        }
    }

    fun cancelCustomerRide() {
        val rideId = selectedRideId ?: return
        coroutineScope.launch {
            try {
                val response = RetrofitClient.api.cancelRide(rideId, CancelRideRequest("Khách hàng hủy chuyến"))
                if (response.isSuccessful) {
                    customerStompClient?.disconnect()
                    customerStompClient = null
                    bottomNavController.popBackStack("home", inclusive = false)
                } else {
                    showRideMessage(backendError(response.errorBody()?.string(), "Không hủy được chuyến (${response.code()})."))
                }
            } catch (exception: Exception) {
                showRideMessage(exception.message ?: "Không kết nối được máy chủ để hủy chuyến.")
            }
        }
    }

    fun updateDriverAvailability(online: Boolean) {
        if (userId == null) {
            showRideMessage("Không xác định được ID tài xế.")
            return
        }
        coroutineScope.launch {
            isAvailabilityBusy = true
            try {
                val response = RetrofitClient.api.setDriverAvailability(DriverAvailabilityRequest(online))
                if (!response.isSuccessful) {
                    showRideMessage(backendError(response.errorBody()?.string(), "Không cập nhật được trạng thái tài xế (${response.code()})."))
                    return@launch
                }
                isDriverOnline = online
                prefs.edit().putBoolean("driver_online", online).apply()
                if (online) DriverLocationService.start(context, userId)
                else DriverLocationService.stop(context)
            } catch (exception: Exception) {
                showRideMessage(exception.message ?: "Không kết nối được máy chủ.")
            } finally {
                isAvailabilityBusy = false
            }
        }
    }

    fun rejectRideOffer() {
        val offer = currentOffer ?: return
        if (handledOfferId == offer.rideId) return
        handledOfferId = offer.rideId
        coroutineScope.launch {
            try {
                val response = RetrofitClient.api.rejectRideOffer(offer.rideId)
                if (!response.isSuccessful && response.code() !in 404..410) {
                    showRideMessage(backendError(response.errorBody()?.string(), "Không thể bỏ qua chuyến (${response.code()})."))
                }
            } catch (exception: Exception) {
                Log.w("DRIVER_DISPATCH", "Không gửi được phản hồi bỏ qua chuyến", exception)
            } finally {
                currentOffer = null
                if (bottomNavController.currentBackStackEntry?.destination?.route == "driver/request") {
                    bottomNavController.popBackStack("home", inclusive = false)
                }
            }
        }
    }

    fun acceptRideOffer() {
        val offer = currentOffer ?: return
        if (isAcceptingOffer || handledOfferId == offer.rideId) return
        handledOfferId = offer.rideId
        coroutineScope.launch {
            isAcceptingOffer = true
            offerError = null
            try {
                val response = RetrofitClient.api.acceptRideOffer(offer.rideId)
                if (!response.isSuccessful) {
                    offerError = backendError(response.errorBody()?.string(), "Không nhận được chuyến (${response.code()}).")
                    handledOfferId = null
                    return@launch
                }
                selectedRideId = offer.rideId
                rideStatus = "DRIVER_ASSIGNED"
                activeTripStep = 0
                bottomNavController.navigate("driver/active-trip") {
                    popUpTo("driver/request") { inclusive = true }
                    launchSingleTop = true
                }
            } catch (exception: Exception) {
                offerError = exception.message ?: "Không kết nối được máy chủ để nhận chuyến."
                handledOfferId = null
            } finally {
                isAcceptingOffer = false
            }
        }
    }

    fun advanceDriverTrip(step: Int) {
        val rideId = selectedRideId ?: return
        if (isUpdatingTrip) return
        if (step == 2 || step == 5) {
            handoverPhase = if (step == 2) "PICKUP" else "DROPOFF"
            handoverError = null
            bottomNavController.navigate("driver/handover")
            return
        }
        coroutineScope.launch {
            isUpdatingTrip = true
            tripProgressError = null
            try {
                val response = when (step) {
                    0 -> RetrofitClient.api.startArriving(rideId, RideProgressRequest())
                    1 -> RetrofitClient.api.driverArrived(rideId, RideProgressRequest())
                    3 -> RetrofitClient.api.startRide(rideId, RideProgressRequest())
                    4 -> RetrofitClient.api.arriveDestination(rideId, RideProgressRequest())
                    6 -> RetrofitClient.api.completeRide(rideId, RideProgressRequest())
                    else -> return@launch
                }
                if (!response.isSuccessful) {
                    tripProgressError = backendError(response.errorBody()?.string(), "Không cập nhật được chuyến (${response.code()}).")
                    return@launch
                }
                rideStatus = response.body()?.currentStatus ?: rideStatus
                if (step == 6) {
                    currentOffer = null
                    selectedRideId = null
                    activeTripStep = 0
                    bottomNavController.popBackStack("home", inclusive = false)
                } else {
                    activeTripStep = step + 1
                }
            } catch (exception: Exception) {
                tripProgressError = exception.message ?: "Không kết nối được máy chủ để cập nhật chuyến."
            } finally {
                isUpdatingTrip = false
            }
        }
    }

    fun submitHandover(
        odometer: Long,
        fuelLevel: Int?,
        notes: String?,
        photos: Map<String, Uri>
    ) {
        val rideId = selectedRideId ?: return
        coroutineScope.launch {
            isSubmittingHandover = true
            handoverError = null
            try {
                val uploadedPhotos = photos.entries.map { (angle, uri) ->
                    HandoverPhotoRequest(angle, CloudinaryUploader.upload(context, uri))
                }
                val request = CreateHandoverReportRequest(odometer, fuelLevel, notes, uploadedPhotos)
                val response = if (handoverPhase == "PICKUP") {
                    RetrofitClient.api.createPickupHandover(rideId, request)
                } else {
                    RetrofitClient.api.createDropoffHandover(rideId, request)
                }
                if (!response.isSuccessful) {
                    handoverError = backendError(response.errorBody()?.string(), "Không lưu được biên bản bàn giao (${response.code()}).")
                    return@launch
                }
                activeTripStep = if (handoverPhase == "PICKUP") 3 else 6
                rideStatus = if (handoverPhase == "PICKUP") "VEHICLE_HANDOVER" else "VEHICLE_RETURNED"
                bottomNavController.popBackStack()
            } catch (exception: Exception) {
                handoverError = exception.message ?: "Không tải được ảnh bàn giao lên máy chủ."
            } finally {
                isSubmittingHandover = false
            }
        }
    }

    fun confirmPickupHandover() {
        val rideId = selectedRideId ?: return
        coroutineScope.launch {
            try {
                val response = RetrofitClient.api.confirmPickupHandover(rideId, ConfirmHandoverRequest())
                if (response.isSuccessful) showRideMessage("Đã xác nhận tình trạng xe.")
                else showRideMessage(backendError(response.errorBody()?.string(), "Không xác nhận được bàn giao (${response.code()})."))
            } catch (exception: Exception) {
                showRideMessage(exception.message ?: "Không kết nối được máy chủ để xác nhận bàn giao.")
            }
        }
    }

    fun logoutFromMain() {
        if (isLoggingOut) return
        isLoggingOut = true
        customerStompClient?.disconnect()
        customerStompClient = null
        if (isDriver) DriverLocationService.stop(context)

        coroutineScope.launch {
            try {
                if (isDriver) {
                    if (userId == null) throw IllegalStateException("Không xác định được ID tài xế.")
                    val response = RetrofitClient.api.setDriverAvailability(DriverAvailabilityRequest(false))
                    if (!response.isSuccessful) {
                        val message = backendError(
                            response.errorBody()?.string(),
                            "Backend chưa cho phép chuyển tài xế sang ngoại tuyến (${response.code()})."
                        )
                        delay(500)
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        ) {
                            DriverLocationService.start(context, userId)
                        }
                        isLoggingOut = false
                        showRideMessage(message)
                        return@launch
                    }
                }
                isDriverOnline = false
                prefs.edit().putBoolean("driver_online", false).apply()
                onLogout()
            } catch (exception: Exception) {
                if (isDriver && userId != null && (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                ) {
                    delay(500)
                    DriverLocationService.start(context, userId)
                }
                isLoggingOut = false
                showRideMessage(exception.message ?: "Không đồng bộ được trạng thái offline; hãy thử lại.")
            }
        }
    }

    val driverLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) updateDriverAvailability(true)
        else showRideMessage("Cần quyền vị trí để chuyển sang trạng thái online.")
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                DeviceTokenRegistrar.registerCurrentToken(context)
            } catch (exception: Exception) {
                Log.w("PUSH_REGISTRATION", "Không lấy được FCM token", exception)
            }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    LaunchedEffect(isDriver) {
        if (isDriver) {
            RideEventBus.events.collect { event ->
                try {
                    val root = JsonParser.parseString(event.payload).asJsonObject
                    val type = root.get("type")?.asString.orEmpty()
                    val payload = root.getAsJsonObject("payload") ?: return@collect
                    when (type) {
                        "RIDE_OFFER" -> {
                            val incoming = com.google.gson.Gson().fromJson(payload, RideOfferPayload::class.java)
                            currentOffer = incoming
                            handledOfferId = null
                            offerError = null
                            if (bottomNavController.currentBackStackEntry?.destination?.route != "driver/request") {
                                bottomNavController.navigate("driver/request") { launchSingleTop = true }
                            }
                        }
                        "OFFER_EXPIRED", "OFFER_CANCELLED" -> {
                            currentOffer = null
                            if (bottomNavController.currentBackStackEntry?.destination?.route == "driver/request") {
                                bottomNavController.popBackStack("home", inclusive = false)
                            }
                        }
                        "RIDE_UPDATED" -> {
                            val status = payload.get("status")?.asString
                            if (status != null) rideStatus = status
                        }
                    }
                } catch (exception: Exception) {
                    Log.w("DRIVER_DISPATCH", "Không đọc được yêu cầu chuyến", exception)
                }
            }
        }
    }

    LaunchedEffect(isDriver, userId) {
        if (!isDriver || !isDriverOnline || userId == null) return@LaunchedEffect
        val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            updateDriverAvailability(true)
        } else {
            isDriverOnline = false
            prefs.edit().putBoolean("driver_online", false).apply()
            showRideMessage("Cho phép truy cập vị trí để tiếp tục nhận chuyến.")
            updateDriverAvailability(false)
        }
    }

    Scaffold(
        bottomBar = {
            if (items.any { it.route == currentRoute }) {
                NavigationBar {
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
        }
    ) { innerPadding ->

        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // TRANG CHỦ
            composable(BottomNavItem.Home.route) {
                if (isDriver) {
                    DriverHomeScreen(
                        isOnline = isDriverOnline,
                        isBusy = isAvailabilityBusy,
                        onOnlineChange = { online ->
                            if (!online) {
                                updateDriverAvailability(false)
                            } else {
                                val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasLocationPermission) updateDriverAvailability(true)
                                else driverLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    )
                } else {
                    CustomerHomeScreen(
                        onSelectLocation = { bottomNavController.navigate("customer/select-location") },
                        onNotifications = { bottomNavController.navigate(BottomNavItem.Message.route) }
                    )
                }
            }

            // HOẠT ĐỘNG
            composable(BottomNavItem.Activity.route) {
                RideHistoryScreen(driver = isDriver)
            }

            // TIN NHẮN
            composable(BottomNavItem.Message.route) {
                NotificationScreen()
            }

            composable(BottomNavItem.Wallet.route) {
                DriverWalletScreen()
            }

            composable("customer/select-location") {
                SelectLocationScreen(
                    onBack = { bottomNavController.popBackStack() },
                    onConfirm = { pickup, destination ->
                        pickupPlace = pickup
                        destinationPlace = destination
                        estimate = null
                        estimateError = null
                        bookingError = null
                        bottomNavController.navigate("customer/options")
                    }
                )
            }

            composable("customer/options") {
                TripOptionsScreen(
                    pickup = pickupPlace,
                    destination = destinationPlace,
                    initialVehicleId = prefs.getString("customer_vehicle_id", null)?.toLongOrNull(),
                    estimate = estimate,
                    estimatedVehicleId = estimatedVehicleId,
                    isEstimating = isEstimating,
                    estimateError = estimateError,
                    bookingError = bookingError,
                    onBack = { bottomNavController.popBackStack() },
                    onEstimate = { vehicleId -> estimateRide(vehicleId) },
                    onBook = { vehicleId, paymentMethod -> createRide(vehicleId, paymentMethod) }
                )
            }

            composable("customer/finding-driver") {
                FindingDriverScreen(
                    rideId = selectedRideId ?: 0L,
                    pickup = pickupPlace,
                    destination = destinationPlace,
                    onCancel = { cancelCustomerRide() }
                )
            }

            composable("customer/tracking") {
                TripTrackingScreen(
                    pickup = pickupPlace,
                    destination = destinationPlace,
                    status = rideStatus,
                    driverName = driverName,
                    driverPhone = driverPhone,
                    driverRating = driverRating,
                    driverLocationText = driverLocationText,
                    onConfirmPickupHandover = { confirmPickupHandover() },
                    onCancel = { cancelCustomerRide() },
                    onBack = { bottomNavController.popBackStack() }
                )
            }

            composable("customer/rating") {
                RatingScreen(fare = finalFare, onDone = {
                    bottomNavController.popBackStack("home", inclusive = false)
                })
            }

            composable("driver/request") {
                IncomingTripScreen(
                    offer = currentOffer ?: return@composable,
                    isAccepting = isAcceptingOffer,
                    error = offerError,
                    onReject = { rejectRideOffer() },
                    onAccept = { acceptRideOffer() }
                )
            }

            composable("driver/active-trip") {
                DriverActiveTripScreen(
                    rideId = selectedRideId ?: currentOffer?.rideId ?: 0L,
                    offer = currentOffer ?: return@composable,
                    step = activeTripStep,
                    isUpdating = isUpdatingTrip,
                    error = tripProgressError,
                    onAdvance = { step -> advanceDriverTrip(step) },
                    onBack = { bottomNavController.popBackStack() }
                )
            }

            composable("driver/handover") {
                RideHandoverScreen(
                    rideId = selectedRideId ?: 0L,
                    phase = handoverPhase,
                    isSubmitting = isSubmittingHandover,
                    error = handoverError,
                    onBack = { bottomNavController.popBackStack() },
                    onSubmit = { odometer, fuel, notes, photos -> submitHandover(odometer, fuel, notes, photos) }
                )
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
                        onClick = { logoutFromMain() },
                        enabled = !isLoggingOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoggingOut) "Đang đồng bộ và đăng xuất…" else "Đăng xuất")
                    }
                }
            }
        }
    }
}
