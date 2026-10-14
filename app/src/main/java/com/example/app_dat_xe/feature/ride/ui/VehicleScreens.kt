package com.example.app_dat_xe.feature.ride.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class CustomerVehicleRef(val id: Long, val label: String)

object CustomerVehicleStore {
    private const val KEY = "customer_vehicle_refs"

    fun load(context: Context): List<CustomerVehicleRef> = try {
        val raw = context.getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optLong("id", 0L)
            if (id <= 0L) null else CustomerVehicleRef(id, item.optString("label").ifBlank { "Xe #$id" })
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(context: Context, vehicles: List<CustomerVehicleRef>) {
        val array = JSONArray()
        vehicles.distinctBy { it.id }.forEach { vehicle ->
            array.put(JSONObject().put("id", vehicle.id).put("label", vehicle.label))
        }
        context.getSharedPreferences("app_dat_xe", Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MyVehiclesScreen(
    vehicles: List<CustomerVehicleRef>,
    defaultVehicleId: Long?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (CustomerVehicleRef) -> Unit,
    onRemove: (CustomerVehicleRef) -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Xe của tôi") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") } },
            actions = { IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Thêm xe") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lưu ID xe đã đăng ký với GoRide để chọn nhanh khi đặt chuyến.", style = MaterialTheme.typography.bodyMedium)
            if (vehicles.isEmpty()) {
                Text("Bạn chưa lưu xe nào. Thêm ID xe do hệ thống cấp để tiếp tục.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(vehicles, key = { it.id }) { vehicle ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(vehicle) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(vehicle.label, fontWeight = FontWeight.SemiBold)
                                Text("ID xe ${vehicle.id}${if (vehicle.id == defaultVehicleId) " · Mặc định" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onSelect(vehicle) }) { Text("Chọn") }
                            IconButton(onClick = { onRemove(vehicle) }) { Icon(Icons.Default.Delete, contentDescription = "Xóa xe") }
                        }
                    }
                }
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Thêm xe") }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddVehicleScreen(onBack: () -> Unit, onSave: (CustomerVehicleRef) -> Unit) {
    var idText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val id = idText.toLongOrNull()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Thêm xe") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Backend hiện chưa có API đăng ký xe. Màn hình này lưu ID xe đã được cấp để dùng lại khi đặt chuyến.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = idText,
                onValueChange = { idText = it.filter(Char::isDigit) },
                label = { Text("ID xe đã đăng ký") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Tên gợi nhớ (ví dụ: Xe gia đình)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { id?.let { onSave(CustomerVehicleRef(it, label.trim().ifBlank { "Xe #$it" })) } },
                enabled = id != null && id > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Lưu xe") }
        }
    }
}
