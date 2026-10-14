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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.app_dat_xe.data.remote.LinkPhoneRequest
import com.example.app_dat_xe.data.remote.LoginResponse
import com.example.app_dat_xe.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun LinkPhoneScreen(
    provider: String,
    role: String,
    onNavigateToOtp: (String) -> Unit,
    onLoginSuccess: (LoginResponse) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var isLinking by remember { mutableStateOf(false) }

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
                if (phoneNumber.isBlank() || isLinking) return@Button

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
                                if (isLinking) return
                                isLinking = true
                                scope.launch {
                                    try {
                                        val user = auth.currentUser
                                            ?: throw IllegalStateException("Không tìm thấy tài khoản mạng xã hội")
                                        user.linkWithCredential(credential).await()
                                        val phoneToken = user.getIdToken(true).await().token
                                            ?: throw IllegalStateException("Không lấy được token số điện thoại")
                                        val providerToken = OtpData.providerIdToken
                                            ?: throw IllegalStateException("Không tìm thấy token tài khoản mạng xã hội")
                                        val response = RetrofitClient.api.linkPhone(
                                            LinkPhoneRequest(providerToken, phoneToken, formattedPhone, role)
                                        )
                                        if (!response.isSuccessful) {
                                            throw IllegalStateException("Không liên kết được số điện thoại (${response.code()})")
                                        }
                                        val login = response.body()
                                            ?: throw IllegalStateException("Máy chủ chưa trả về tài khoản")
                                        OtpData.clear()
                                        onLoginSuccess(login)
                                    } catch (exception: Exception) {
                                        isLinking = false
                                        Toast.makeText(context, exception.message ?: "Liên kết số điện thoại thất bại", Toast.LENGTH_LONG).show()
                                    }
                                }
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
                                OtpData.forceResendingToken = token
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
