package com.example.app_dat_xe.feature.ride.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DriverOnboardingScreen(
    initialLicenseNumber: String,
    initialLicenseClass: String,
    initialExperience: String,
    initialPhoto: String?,
    approvalStatus: String,
    onBack: () -> Unit,
    onSaveDraft: (String, String, String, String?) -> Unit
) {
    var licenseNumber by remember { mutableStateOf(initialLicenseNumber) }
    var licenseClass by remember { mutableStateOf(initialLicenseClass) }
    var experience by remember { mutableStateOf(initialExperience) }
    var licensePhoto by remember { mutableStateOf(initialPhoto?.let(Uri::parse)) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) licensePhoto = uri
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Hồ sơ tài xế") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                when (approvalStatus) {
                    "APPROVED" -> "Hồ sơ tài xế đã được duyệt."
                    "REJECTED" -> "Hồ sơ bị từ chối. Hãy cập nhật thông tin và gửi lại khi backend hỗ trợ."
                    else -> "Hồ sơ đang chờ duyệt. Tài xế chưa được bật trạng thái online."
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text("Backend hiện chưa có API onboarding tài xế; thông tin bên dưới được lưu thành bản nháp trên thiết bị.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = licenseNumber,
                onValueChange = { licenseNumber = it },
                label = { Text("Số giấy phép lái xe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = licenseClass,
                onValueChange = { licenseClass = it.uppercase().take(10) },
                label = { Text("Hạng bằng (ví dụ: B1, B2, C)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = experience,
                onValueChange = { experience = it.filter(Char::isDigit).take(2) },
                label = { Text("Số năm kinh nghiệm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (licensePhoto == null) "Chọn ảnh giấy phép" else "Đổi ảnh giấy phép")
            }
            licensePhoto?.let { AsyncImage(model = it, contentDescription = "Ảnh giấy phép", modifier = Modifier.fillMaxWidth().height(210.dp)) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onSaveDraft(licenseNumber.trim(), licenseClass.trim(), experience, licensePhoto?.toString()) },
                enabled = licenseNumber.isNotBlank() && licenseClass.isNotBlank() && experience.isNotBlank() && licensePhoto != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Lưu bản nháp") }
        }
    }
}
