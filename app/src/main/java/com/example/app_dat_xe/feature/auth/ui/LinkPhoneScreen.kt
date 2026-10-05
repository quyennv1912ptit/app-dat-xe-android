package com.example.app_dat_xe.feature.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun LinkPhoneScreen(provider: String, onNavigateToOtp: (String) -> Unit
                    , onLoginSuccess: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Liên kết số điện thoại",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bạn đang đăng nhập bằng $provider. Vui lòng cung cấp số điện thoại để tài xế có thể liên lạc với bạn khi đặt xe.",
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Nhập số điện thoại") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (phoneNumber.isBlank()) return@Button

                val formattedPhone =
                    if (phoneNumber.startsWith("0")) {
                        "+84" + phoneNumber.substring(1)
                    } else {
                        phoneNumber
                    }
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(
                        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            override fun onVerificationCompleted(
                                credential: PhoneAuthCredential
                            ) {
                            }
                            override fun onVerificationFailed(
                                e: FirebaseException
                            ) {
                                Toast.makeText(
                                    context,
                                    "Gửi OTP thất bại: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            override fun onCodeSent(
                                verificationId: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                OtpData.verificationId = verificationId
                                onNavigateToOtp(formattedPhone)
                            }
                        }
                    )
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        ) {
            Text("Tiếp tục", style = MaterialTheme.typography.titleMedium)
        }
    }
}