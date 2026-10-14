package com.example.app_dat_xe.feature.ride.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.app_dat_xe.data.location.DeviceLocationProvider
import com.example.app_dat_xe.data.remote.EstimateRideResponse
import com.example.app_dat_xe.data.remote.RetrofitClient
import com.example.app_dat_xe.data.remote.RideOfferPayload
import com.example.app_dat_xe.data.remote.RideSummaryData
import com.example.app_dat_xe.feature.ride.formatVnd
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RideGreen = Color(0xFF0B8F68)
private val RideGreenDark = Color(0xFF07684C)
private val RideAmber = Color(0xFFFFB547)

data class RidePlace(val address: String, val latitude: Double? = null, val longitude: Double? = null)
private data class PlaceSuggestion(val name: String, val address: String, val latitude: Double, val longitude: Double)
private fun openDialer(context: android.content.Context, phoneNumber: String?) {
    if (phoneNumber.isNullOrBlank()) {
        Toast.makeText(context, "Chưa có số điện thoại liên hệ.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)))
    } catch (_: Exception) {
        Toast.makeText(context, "Không mở được ứng dụng gọi điện.", Toast.LENGTH_SHORT).show()
    }
}
private fun openMapAt(context: android.content.Context, latitude: Double, longitude: Double) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?z=16"))
        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
        else Toast.makeText(context, "Thiết bị chưa có ứng dụng bản đồ.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Không mở được ứng dụng bản đồ.", Toast.LENGTH_SHORT).show()
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Đã đặt lịch · chờ đến giờ tìm tài xế"
    "SEARCHING" -> "Đang tìm tài xế"
    "DRIVER_ASSIGNED" -> "Tài xế đã nhận chuyến"
    "DRIVER_ARRIVING" -> "Tài xế đang đến điểm đón"
    "DRIVER_ARRIVED" -> "Tài xế đã đến điểm đón"
    "VEHICLE_HANDOVER" -> "Xác nhận bàn giao xe"
    "IN_PROGRESS" -> "Chuyến đi đang diễn ra"
    "ARRIVED_DESTINATION" -> "Tài xế đã đến điểm trả"
    "VEHICLE_RETURNED" -> "Đang hoàn tất bàn giao xe"
    "COMPLETED" -> "Chuyến đi hoàn tất"
    "CANCELLED" -> "Chuyến đi đã bị hủy"
    "NO_DRIVER_FOUND", "EXPIRED" -> "Không tìm thấy tài xế"
    else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private val placeSuggestions = listOf(
    PlaceSuggestion("Chợ Bến Thành", "Lê Lợi, phường Bến Thành, Quận 1", 10.7725, 106.6980),
    PlaceSuggestion("Nhà thờ Đức Bà", "Công xã Paris, phường Sài Gòn, Quận 1", 10.7798, 106.6990),
    PlaceSuggestion("Landmark 81", "Vinhomes Central Park, Bình Thạnh", 10.7951, 106.7218),
    PlaceSuggestion("Sân bay Tân Sơn Nhất", "Trường Sơn, phường Tân Sơn Hòa, Tân Bình", 10.8188, 106.6519),
    PlaceSuggestion("Bưu điện Trung tâm", "02 Công xã Paris, phường Sài Gòn, Quận 1", 10.7797, 106.6991)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    pickup: RidePlace,
    onSelectLocation: () -> Unit,
    onNotifications: () -> Unit,
    onPickupLocationUpdated: (RidePlace) -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    fun refreshLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Toast.makeText(context, "Hãy cấp quyền vị trí để cập nhật điểm đón.", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val location = DeviceLocationProvider.currentLocation(context)
            if (location == null) {
                Toast.makeText(context, "Không lấy được vị trí GPS. Hãy bật định vị rồi thử lại.", Toast.LENGTH_SHORT).show()
            } else {
                onPickupLocationUpdated(RidePlace("Vị trí hiện tại", location.latitude, location.longitude))
                openMapAt(context, location.latitude, location.longitude)
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) refreshLocation()
        else Toast.makeText(context, "Cần quyền vị trí để cập nhật điểm đón.", Toast.LENGTH_SHORT).show()
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("GoRide", Modifier.padding(horizontal = 24.dp, vertical = 26.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = RideGreenDark)
                NavigationDrawerItem(
                    label = { Text("Đặt chuyến") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() }; onSelectLocation() },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Thông báo") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() }; onNotifications() },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
            }
        }
    ) {
      Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MapUnavailablePlaceholder()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Mở menu", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VỊ TRÍ CỦA BẠN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    pickup.latitude?.let { lat -> pickup.longitude?.let { lng -> "%.5f, %.5f".format(lat, lng) } } ?: pickup.address,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(shape = CircleShape, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Default.Notifications, contentDescription = "Thông báo", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.align(Alignment.End),
                shape = CircleShape,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                IconButton(onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) refreshLocation()
                    else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Định vị lại", tint = RideGreen)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectLocation),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Xin chào, bạn muốn đi đâu?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = RideGreen)
                        Spacer(Modifier.width(12.dp))
                        Text("Bạn muốn đi đâu?", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun MapUnavailablePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = RideGreen, modifier = Modifier.size(32.dp))
            Text("Bản đồ trực tiếp chưa được cấu hình", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("Vị trí GPS vẫn có thể dùng để chọn điểm đón.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SelectLocationScreen(
    onBack: () -> Unit,
    onConfirm: (pickup: RidePlace, destination: RidePlace) -> Unit,
    initialPickup: RidePlace? = null
) {
    val context = LocalContext.current
    var pickup by remember(initialPickup) {
        mutableStateOf(initialPickup?.takeIf { it.latitude != null && it.longitude != null } ?: RidePlace("Đang xác định vị trí…"))
    }
    var destination by remember { mutableStateOf("") }
    var selectedDestination by remember { mutableStateOf<RidePlace?>(null) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(placeSuggestions) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
        if (hasLocationPermission) {
            coroutineScope.launch {
                val location = DeviceLocationProvider.currentLocation(context)
                if (location != null) {
                    pickup = RidePlace("Vị trí hiện tại", location.latitude, location.longitude)
                    locationError = null
                } else {
                    locationError = "Không lấy được GPS. Hãy bật định vị rồi thử lại."
                }
            }
        } else {
            locationError = "Cần quyền vị trí để đặt điểm đón hiện tại."
        }
    }
    val destinationFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250)
        destinationFocusRequester.requestFocus()
        if (pickup.latitude != null && pickup.longitude != null) return@LaunchedEffect
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            val location = DeviceLocationProvider.currentLocation(context)
            if (location != null) pickup = RidePlace("Vị trí hiện tại", location.latitude, location.longitude)
            else locationError = "Không lấy được GPS. Hãy bật định vị rồi thử lại."
        }
    }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResults = placeSuggestions
        } else {
            delay(350)
            searchResults = try {
                val response = RetrofitClient.api.geocodeAddress(query)
                val body = response.body()
                val geocoding = if (body != null && body.isJsonPrimitive && body.asJsonPrimitive.isString) {
                    JsonParser.parseString(body.asString)
                } else body
                val features = geocoding?.takeIf { it.isJsonObject }
                    ?.asJsonObject?.getAsJsonArray("features")
                val geocoded = features?.mapNotNull { element ->
                    if (!element.isJsonObject) return@mapNotNull null
                    val feature = element.asJsonObject
                    val center = feature.getAsJsonArray("center") ?: return@mapNotNull null
                    if (center.size() < 2) return@mapNotNull null
                    val name = feature.get("text")?.takeUnless { it.isJsonNull }?.asString ?: query
                    val address = feature.get("place_name")?.takeUnless { it.isJsonNull }?.asString ?: name
                    PlaceSuggestion(name, address, center[1].asDouble, center[0].asDouble)
                }
                geocoded?.takeIf { it.isNotEmpty() } ?: run {
                    placeSuggestions.filter { (it.name + it.address).contains(query, ignoreCase = true) }
                }
            } catch (_: Exception) {
                placeSuggestions.filter { (it.name + it.address).contains(query, ignoreCase = true) }
            }
        }
    }
    Scaffold(topBar = { RideTopBar("Chọn điểm đến", onBack) }, containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pickup.address,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Điểm đón") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RideGreen) },
                    singleLine = true,
                    readOnly = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; destination = it; selectedDestination = null },
                    modifier = Modifier.fillMaxWidth().focusRequester(destinationFocusRequester),
                    label = { Text("Điểm đến") },
                    placeholder = { Text("Nhập tên địa điểm") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RideAmber) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = RideGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Điểm đón lấy từ GPS của thiết bị", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Làm mới") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                items(searchResults) { place ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            query = place.name
                            destination = place.name
                            selectedDestination = RidePlace(place.address, place.latitude, place.longitude)
                        }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RideGreen) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(place.address, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 70.dp))
                }
                item {
                    Box(Modifier.fillMaxWidth().height(130.dp).padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(18.dp))) {
                        MapUnavailablePlaceholder()
                    }
                }
            }
            Button(
                onClick = { selectedDestination?.let { onConfirm(pickup, it) } },
                enabled = pickup.latitude != null && selectedDestination != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RideGreen)
            ) { Text("Xác nhận điểm đón/đến", fontWeight = FontWeight.Bold) }
            locationError?.let { Text(it, Modifier.fillMaxWidth().padding(bottom = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TripOptionsScreen(
    pickup: RidePlace,
    destination: RidePlace,
    initialVehicleId: Long?,
    vehicles: List<CustomerVehicleRef>,
    estimate: EstimateRideResponse?,
    estimatedVehicleId: Long?,
    isEstimating: Boolean,
    estimateError: String?,
    bookingError: String?,
    onBack: () -> Unit,
    onManageVehicles: () -> Unit,
    onVehicleSelected: (Long) -> Unit,
    onEstimate: (Long) -> Unit,
    onBook: (vehicleId: Long, paymentMethod: String) -> Unit
) {
    var promo by remember { mutableStateOf("") }
    var selectedVehicleId by remember(initialVehicleId) { mutableStateOf(initialVehicleId?.takeIf { it > 0 }) }
    var showVehiclePicker by remember { mutableStateOf(false) }
    val vehicleId = selectedVehicleId
    val selectedVehicle = vehicles.firstOrNull { it.id == vehicleId }
    val currentEstimate = estimate?.takeIf { estimatedVehicleId == vehicleId }
    Scaffold(topBar = { RideTopBar("Chọn dịch vụ", onBack) }, containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MapUnavailablePlaceholder()
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(MaterialTheme.colorScheme.surface).padding(bottom = 12.dp)) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Lộ trình của bạn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    RouteLine(pickup.address, destination.address)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            currentEstimate?.let { "${"%.1f".format(it.distanceKm)} km · khoảng ${it.estimatedDurationMinutes} phút" }
                                ?: "Tính quãng đường và giá theo lộ trình",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Giá từ máy chủ", style = MaterialTheme.typography.labelSmall, color = RideGreen)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedButton(
                    onClick = { showVehiclePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(selectedVehicle?.let { "${it.label} · ID ${it.id}" } ?: vehicleId?.let { "Xe #$it" } ?: "Chọn xe đã lưu")
                }
                OutlinedButton(
                    onClick = { vehicleId?.let(onEstimate) },
                    enabled = vehicleId != null && vehicleId > 0 && !isEstimating,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (isEstimating) "Đang tính giá…" else "Tính giá chuyến đi") }
                estimateError?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = RideGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Thanh toán tiền mặt", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedTextField(value = promo, onValueChange = { promo = it }, modifier = Modifier.weight(1.2f).height(54.dp), placeholder = { Text("Mã ưu đãi", fontSize = 12.sp) }, singleLine = true, shape = RoundedCornerShape(14.dp), enabled = false)
                }
                bookingError?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { vehicleId?.let { onBook(it, "CASH") } }, enabled = vehicleId != null && vehicleId > 0 && currentEstimate != null, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen)) {
                    Text("Đặt xe ngay · ${formatVnd(currentEstimate?.estimatedFare)}đ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showVehiclePicker) {
        ModalBottomSheet(onDismissRequest = { showVehiclePicker = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Chọn xe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (vehicles.isEmpty()) Text("Chưa có xe đã lưu. Thêm ID xe do hệ thống cấp.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                vehicles.forEach { vehicle ->
                    OutlinedButton(
                        onClick = {
                            selectedVehicleId = vehicle.id
                            onVehicleSelected(vehicle.id)
                            showVehiclePicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("${vehicle.label} · ID ${vehicle.id}") }
                }
                OutlinedButton(onClick = { showVehiclePicker = false; onManageVehicles() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vehicles.isEmpty()) "Thêm xe" else "Quản lý xe")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun FindingDriverScreen(
    rideId: Long,
    pickup: RidePlace,
    destination: RidePlace,
    onCancel: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(1_600), RepeatMode.Reverse), label = "pulse")
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") } }
            Spacer(Modifier.height(25.dp))
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(190.dp * pulse).clip(CircleShape).background(RideGreen.copy(alpha = .07f)))
                Box(Modifier.size(135.dp * pulse).clip(CircleShape).background(RideGreen.copy(alpha = .11f)))
                Surface(shape = CircleShape, color = RideGreen, shadowElevation = 8.dp, modifier = Modifier.size(78.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp)) }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("Đang tìm tài xế gần nhất…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Đang tìm tài xế cho chuyến đi của bạn", modifier = Modifier.padding(top = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(25.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    RouteLine(pickup.address, destination.address)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mã chuyến #$rideId · Chờ phản hồi tài xế", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("Đã báo giá", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Hủy yêu cầu", color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun TripTrackingScreen(
    rideId: Long,
    pickup: RidePlace,
    destination: RidePlace,
    status: String,
    driverName: String?,
    driverPhone: String?,
    driverRating: Double?,
    driverLocationText: String?,
    onConfirmPickupHandover: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        MapUnavailablePlaceholder()
        Surface(Modifier.align(Alignment.TopStart).padding(15.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(statusLabel(status), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(driverLocationText?.let { "Vị trí tài xế: $it" } ?: "Cập nhật trực tiếp từ tài xế", color = RideGreen, style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(52.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, tint = RideGreen) } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(driverName ?: "Đang tải thông tin tài xế", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${driverPhone ?: "Số điện thoại chưa có"} · ★ ${driverRating?.let { "%.1f".format(it) } ?: "–"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { openDialer(context, driverPhone) }) { Icon(Icons.Default.Call, contentDescription = "Gọi tài xế", tint = RideGreen) }
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hành trình GoRide #$rideId: ${pickup.address} → ${destination.address}. Trạng thái: ${statusLabel(status)}."
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ hành trình"))
                }) { Icon(Icons.Default.Share, contentDescription = "Chia sẻ hành trình", tint = RideGreen) }
            }
            RouteLine(pickup.address, destination.address)
            if (status == "VEHICLE_HANDOVER") {
                Button(onClick = onConfirmPickupHandover, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen)) {
                    Text("Xác nhận tình trạng xe", fontWeight = FontWeight.Bold)
                }
            }
            if (status in setOf("PENDING", "SEARCHING", "DRIVER_ASSIGNED", "DRIVER_ARRIVING", "DRIVER_ARRIVED")) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Hủy chuyến", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun RatingScreen(fare: Long?, onDone: () -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(76.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RideGreen, modifier = Modifier.size(42.dp)) } }
            Spacer(Modifier.height(18.dp))
            Text("Chuyến đi hoàn tất", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Cảm ơn bạn đã đồng hành cùng GoRide", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Máy chủ chưa có API lưu đánh giá; nội dung bạn nhập chưa được gửi.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(27.dp))
            Text("${formatVnd(fare)}đ", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = RideGreenDark)
            Text("Thông tin thanh toán từ chuyến đi", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(28.dp))
            Text("Bạn thấy tài xế thế nào?", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Star, contentDescription = "$star sao", tint = if (star <= rating) RideAmber else Color(0xFFD5DDD8), modifier = Modifier.size(34.dp))
                    }
                }
            }
            OutlinedTextField(value = comment, onValueChange = { comment = it }, modifier = Modifier.fillMaxWidth().height(118.dp), placeholder = { Text("Chia sẻ trải nghiệm của bạn…") }, shape = RoundedCornerShape(16.dp), maxLines = 4)
            Spacer(Modifier.weight(1f))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen)) { Text("Về trang chủ", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun NotificationScreen() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Text("Thông báo", Modifier.padding(horizontal = 20.dp, vertical = 20.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = RideGreen, modifier = Modifier.size(40.dp))
                Text("Chưa có thông báo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Thông báo chuyến đi sẽ xuất hiện ở đây khi được kết nối với hộp thư của máy chủ.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DriverHomeScreen(
    isOnline: Boolean,
    isBusy: Boolean,
    approvalStatus: String,
    onOpenOnboarding: () -> Unit,
    onOnlineChange: (Boolean) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val approved = approvalStatus == "APPROVED"
    val approvalLabel = when (approvalStatus) {
        "APPROVED" -> "Đã duyệt"
        "REJECTED" -> "Bị từ chối"
        else -> "Chờ duyệt"
    }
    fun openCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Toast.makeText(context, "Hãy cấp quyền vị trí để định vị.", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val location = DeviceLocationProvider.currentLocation(context)
            if (location == null) {
                Toast.makeText(context, "Không lấy được GPS. Hãy bật định vị rồi thử lại.", Toast.LENGTH_SHORT).show()
            } else {
                openMapAt(context, location.latitude, location.longitude)
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) openCurrentLocation()
        else Toast.makeText(context, "Cần quyền vị trí để định vị.", Toast.LENGTH_SHORT).show()
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("GoRide · Tài xế", Modifier.padding(horizontal = 24.dp, vertical = 26.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = RideGreenDark)
                NavigationDrawerItem(
                    label = { Text("Hoạt động chuyến") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() }; onOpenActivity() },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Ví tài xế") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() }; onOpenWallet() },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Tài khoản") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() }; onOpenAccount() },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }
        }
    ) {
      Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MapUnavailablePlaceholder()
        Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, shadowElevation = 5.dp, color = MaterialTheme.colorScheme.surface) { IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Mở menu") } }
            Surface(shape = RoundedCornerShape(30.dp), color = if (isOnline) RideGreen else MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Row(Modifier.padding(start = 13.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isOnline) "ĐANG ONLINE" else "NGOẠI TUYẾN", color = if (isOnline) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Switch(checked = isOnline, onCheckedChange = onOnlineChange, enabled = !isBusy && approved)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text(if (isOnline) "Bạn đã sẵn sàng nhận chuyến" else "Bật trạng thái để bắt đầu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Trạng thái hồ sơ: $approvalLabel", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (isBusy) "Đang cập nhật trạng thái…" else if (isOnline) "Ứng dụng đang gửi vị trí và chờ yêu cầu mới." else if (approved) "Bạn có thể bật online để nhận chuyến." else "Cần được duyệt hồ sơ trước khi bật online.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onOpenOnboarding, modifier = Modifier.fillMaxWidth()) { Text("Hồ sơ tài xế") }
                }
            }
        }
        Surface(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            IconButton(onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) openCurrentLocation()
                else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Icon(Icons.Default.MyLocation, contentDescription = "Định vị lại", tint = RideGreen) }
        }
      }
    }
}

@Composable
fun IncomingTripScreen(
    offer: RideOfferPayload,
    isAccepting: Boolean,
    error: String?,
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    var seconds by remember(offer.rideId, offer.expiresAt, offer.serverTimeOffsetMs) {
        mutableIntStateOf(((offer.expiresAt - (System.currentTimeMillis() + offer.serverTimeOffsetMs)) / 1_000L).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(offer.rideId, offer.expiresAt, offer.serverTimeOffsetMs, isAccepting) {
        if (isAccepting) return@LaunchedEffect
        while (true) {
            val remaining = ((offer.expiresAt - (System.currentTimeMillis() + offer.serverTimeOffsetMs) + 999L) / 1_000L)
                .toInt()
                .coerceAtLeast(0)
            seconds = remaining
            if (remaining == 0) {
                onReject()
                break
            }
            delay(1_000)
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Yêu cầu chuyến mới", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Surface(shape = CircleShape, color = RideGreen) { Text("${seconds}s", Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold) }
            }
            LinearProgressIndicator(progress = { seconds / 15f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = RideGreen, trackColor = MaterialTheme.colorScheme.outlineVariant)
            Text("Yêu cầu từ ${offer.customer.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Bạn nhận được", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formatVnd(offer.driverEarning)}đ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = RideGreenDark)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RouteLine(offer.pickup.address, offer.dropoff.address)
                    Text("Cách điểm đón ${"%.1f".format(offer.distanceToPickupKm)} km · Quãng đường ${"%.1f".format(offer.tripDistanceKm)} km · ${offer.etaToPickupMin} phút", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = RideGreen)
                            Spacer(Modifier.width(9.dp))
                            Column { Text(offer.customer.displayName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text("Đánh giá ${"%.1f".format(offer.customer.rating)} ★", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp))) { MapUnavailablePlaceholder() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReject, enabled = !isAccepting && seconds > 0, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(16.dp)) { Text("BỎ QUA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = onAccept, enabled = !isAccepting && seconds > 0, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen)) { Text(if (isAccepting) "ĐANG NHẬN…" else "NHẬN CHUYẾN", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun DriverActiveTripScreen(
    rideId: Long,
    offer: RideOfferPayload,
    step: Int,
    pickupHandoverConfirmed: Boolean,
    isUpdating: Boolean,
    error: String?,
    onAdvance: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val labels = listOf("BẮT ĐẦU ĐI ĐÓN", "ĐÃ ĐẾN ĐIỂM ĐÓN", "CHỤP ẢNH XE TRƯỚC CHUYẾN", "BẮT ĐẦU HÀNH TRÌNH", "ĐÃ ĐẾN ĐIỂM TRẢ", "CHỤP ẢNH XE SAU CHUYẾN", "HOÀN THÀNH CHUYẾN ĐI")
    val statuses = listOf("Xác nhận bắt đầu đón khách", "Đến điểm đón", "Ghi nhận tình trạng xe trước chuyến", "Khách đã lên xe", "Đến điểm trả", "Ghi nhận tình trạng xe sau chuyến", "Hoàn thành chuyến #$rideId")
    Box(Modifier.fillMaxSize()) {
        MapUnavailablePlaceholder()
        Surface(Modifier.align(Alignment.TopStart).padding(14.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") } }
        Surface(Modifier.align(Alignment.TopEnd).padding(14.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Text(
                "Mở điều hướng ↗",
                Modifier.clickable {
                    val target = if (step <= 1) offer.pickup else offer.dropoff
                    openMapAt(context, target.lat, target.lng)
                }.padding(horizontal = 14.dp, vertical = 11.dp),
                color = RideGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(statuses[step.coerceIn(0, statuses.lastIndex)], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, tint = RideGreen) } }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(offer.customer.displayName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text("Khách hàng · ★ ${"%.1f".format(offer.customer.rating)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { openDialer(context, offer.customer.phoneNumber) }) { Icon(Icons.Default.Call, contentDescription = "Gọi khách", tint = RideGreen) }
            }
            RouteLine(offer.pickup.address, offer.dropoff.address)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            val waitingForHandoverConfirmation = step == 3 && !pickupHandoverConfirmed
            if (waitingForHandoverConfirmation) {
                Text("Đang chờ khách xác nhận biên bản nhận xe…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { onAdvance(step) }, enabled = !isUpdating && !waitingForHandoverConfirmation && step in labels.indices, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen)) {
                Text(if (isUpdating) "ĐANG CẬP NHẬT…" else if (waitingForHandoverConfirmation) "CHỜ KHÁCH XÁC NHẬN" else labels[step.coerceIn(0, labels.lastIndex)], fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RideHandoverScreen(
    rideId: Long,
    phase: String,
    initialOdometer: Long? = null,
    isSubmitting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (odometer: Long, fuelLevel: Int?, notes: String?, photos: Map<String, Uri>) -> Unit
) {
    val angles = listOf(
        "FRONT" to "Mặt trước",
        "BACK" to "Mặt sau",
        "LEFT" to "Bên trái",
        "RIGHT" to "Bên phải",
        "DASHBOARD" to "Đồng hồ xe (tùy chọn)"
    )
    var photos by remember(rideId, phase) { mutableStateOf<Map<String, Uri>>(emptyMap()) }
    var pendingAngle by remember { mutableStateOf<String?>(null) }
    var odometerText by remember(rideId, phase, initialOdometer) {
        mutableStateOf(if (phase == "DROPOFF") initialOdometer?.toString().orEmpty() else "")
    }
    var fuelLevelText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val angle = pendingAngle
        if (uri != null && angle != null) photos = photos + (angle to uri)
        pendingAngle = null
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val angle = pendingAngle
        val uri = cameraUri
        if (saved && angle != null && uri != null) {
            photos = photos + (angle to uri)
            photoError = null
        } else if (!saved) {
            photoError = "Không chụp được ảnh. Hãy thử lại hoặc chọn ảnh từ thiết bị."
        }
        pendingAngle = null
        cameraUri = null
    }
    fun capturePhoto(angle: String) {
        try {
            val folder = File(context.cacheDir, "handover").apply { mkdirs() }
            val imageFile = File.createTempFile("${angle.lowercase()}_", ".jpg", folder)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            pendingAngle = angle
            cameraUri = uri
            photoError = null
            cameraLauncher.launch(uri)
        } catch (exception: Exception) {
            photoError = exception.message ?: "Không mở được camera."
        }
    }
    val odometer = odometerText.toLongOrNull()
    val isPickup = phase == "PICKUP"
    Scaffold(topBar = { RideTopBar(if (isPickup) "Bàn giao tại điểm đón" else "Bàn giao tại điểm trả", onBack) }, containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text("Chuyến #$rideId · Chụp đủ 4 góc xe để lưu biên bản.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Số km trên đồng hồ *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = fuelLevelText,
                onValueChange = { fuelLevelText = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mức nhiên liệu (0–100, tùy chọn)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Text("Ảnh xe", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            angles.forEach { (angle, title) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { pendingAngle = angle; photoPicker.launch("image/*") },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (photos.containsKey(angle)) "$title · Đã chọn" else "Chọn ảnh · $title")
                    }
                    OutlinedButton(
                        onClick = { capturePhoto(angle) },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Chụp") }
                }
                photos[angle]?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Ảnh xem trước: $title",
                        modifier = Modifier.size(92.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }
            photoError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ghi chú tình trạng xe") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp)
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = {
                    val fuel = fuelLevelText.toIntOrNull()
                    val requiredAngles = angles.map { it.first }.filter { it != "DASHBOARD" }
                    if (odometer != null && odometer >= 0 && photos.keys.containsAll(requiredAngles) && (fuel == null || fuel in 0..100)) {
                        onSubmit(odometer, fuel, notes.trim().ifBlank { null }, photos)
                    }
                },
                enabled = !isSubmitting && odometer != null && odometer >= 0 && photos.keys.containsAll(angles.map { it.first }.filter { it != "DASHBOARD" }) && (fuelLevelText.isBlank() || fuelLevelText.toIntOrNull()?.let { it in 0..100 } == true),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RideGreen)
            ) {
                Text(if (isSubmitting) "ĐANG TẢI ẢNH VÀ LƯU…" else "LƯU BIÊN BẢN BÀN GIAO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DriverWalletScreen() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ví tài xế", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(21.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = RideGreen, modifier = Modifier.size(38.dp))
                Text("Ví chưa được kết nối", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Số dư, lịch sử giao dịch và thao tác nạp/rút sẽ hiển thị khi dịch vụ ví được kết nối.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun RideHistoryScreen(driver: Boolean) {
    var rides by remember { mutableStateOf<List<RideSummaryData>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(driver, reloadKey) {
        isLoading = true
        error = null
        try {
            val allRides = mutableListOf<RideSummaryData>()
            var page = 0
            while (true) {
                val response = RetrofitClient.api.listMyRides(page = page, size = 100)
                if (!response.isSuccessful) {
                    error = "Không tải được lịch sử chuyến (${response.code()})."
                    break
                }
                val ridePage = response.body()
                if (ridePage == null) {
                    error = "Máy chủ không trả về lịch sử chuyến."
                    break
                }
                allRides += ridePage.content
                if (!ridePage.hasNext || ridePage.content.isEmpty() || page >= 100) break
                page += 1
            }
            if (error == null) rides = allRides
        } catch (exception: Exception) {
            error = exception.message ?: "Không kết nối được máy chủ để tải lịch sử chuyến."
        } finally {
            isLoading = false
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("${if (driver) "Chuyến đã chạy" else "Hoạt động của bạn"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RideGreen) }
            error != null -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { reloadKey++ }) { Text("Thử lại") }
            }
            rides.isNullOrEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có chuyến đi", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rides.orEmpty(), key = { it.id }) { ride ->
                    Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Chuyến #${ride.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Text("${formatVnd(ride.finalFare ?: ride.estimatedFare)}đ", color = RideGreen, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${ride.pickupAddress ?: "Chưa có điểm đón"} → ${ride.dropoffAddress ?: "Chưa có điểm trả"}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(statusLabel(ride.status), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RideTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = MaterialTheme.colorScheme.onSurface) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun RouteLine(pickup: String, destination: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(RideGreen))
            Box(Modifier.width(1.dp).height(25.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Box(Modifier.size(9.dp).clip(CircleShape).background(RideAmber))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pickup, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(destination, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
