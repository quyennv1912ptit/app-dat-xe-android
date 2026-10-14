package com.example.app_dat_xe.feature.auth.ui

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.example.app_dat_xe.data.remote.AvatarRequest
import com.example.app_dat_xe.data.remote.LinkPhoneRequest
import com.example.app_dat_xe.data.remote.LoginRequest
import com.example.app_dat_xe.data.remote.LoginResponse
import com.example.app_dat_xe.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun OtpVerifyScreen(phoneNumber: String,
                    role: String,
                    provider: String,
                    onNavigateToSignUp: () -> Unit,
                    onLoginSuccess: (LoginResponse) -> Unit) {
    var otp by remember { mutableStateOf("") }
    var timer by remember { mutableIntStateOf(60) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }

    LaunchedEffect(timer) {
        if (timer > 0) {
            delay(1.seconds)
            timer--
        }
    }
    fun resendOtp() {
        val activity = context as? Activity
        if (activity == null) {
            Toast.makeText(context, "Không thể mở xác thực điện thoại trên màn hình này.", Toast.LENGTH_LONG).show()
            return
        }
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) = Unit

                override fun onVerificationFailed(exception: FirebaseException) {
                    isResending = false
                    Toast.makeText(context, "Gửi lại OTP thất bại: ${exception.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    OtpData.verificationId = verificationId
                    OtpData.forceResendingToken = token
                    otp = ""
                    timer = 60
                    isResending = false
                    Toast.makeText(context, "Đã gửi mã OTP mới.", Toast.LENGTH_SHORT).show()
                    focusRequester.requestFocus()
                }
            })
        OtpData.forceResendingToken?.let(optionsBuilder::setForceResendingToken)
        isResending = true
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }
    fun verifyOtp() {
        val verificationId = OtpData.verificationId

        if (verificationId == null) {
            Toast.makeText(
                context,
                "Không tìm thấy mã xác thực. Vui lòng gửi lại OTP.",
                Toast.LENGTH_LONG
            ).show()

            isLoading = false
            return
        }

        val credential = PhoneAuthProvider.getCredential(
            verificationId,
            otp
        )




        if (provider == "Facebook") {

        }



        val authTask = if (provider == "Facebook" || provider == "Google") {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.linkWithCredential(credential)
            } else {
                auth.signInWithCredential(credential)
            }
        } else {
            auth.signInWithCredential(credential)
        }

        authTask
            .addOnSuccessListener { result ->



                val user = result.user

                if (user == null) {
                    isLoading = false
                    return@addOnSuccessListener
                }



                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->

                        val idToken = tokenResult.token

                        if (idToken == null) {
                            isLoading = false

                            Toast.makeText(
                                context,
                                "Không lấy được Firebase ID Token",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@addOnSuccessListener
                        }

                        OtpData.phoneIdToken = idToken
                        OtpData.phoneNumber = phoneNumber

                        scope.launch {

                            try {

                                val response =
                                    if (provider == "Facebook" || provider == "Google") {

                                        val providerIdToken = OtpData.providerIdToken

                                        if (providerIdToken == null) {
                                            Toast.makeText(
                                                context,
                                                "Không tìm thấy Provider ID Token",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            isLoading = false
                                            return@launch
                                        }

                                        RetrofitClient.api.linkPhone(
                                            LinkPhoneRequest(
                                                providerIdToken = providerIdToken,
                                                phoneIdToken = idToken,
                                                phoneNumber = phoneNumber,
                                                role = role
                                            )
                                        )

                                    } else {

                                        RetrofitClient.api.login(
                                            LoginRequest(
                                                idToken = idToken,
                                                role = role
                                            )
                                        )
                                    }

                                if (response.isSuccessful) {

                                    val loginResult = response.body()

                                    if (loginResult != null) {

                                        if (provider == "Google" || provider == "Facebook") {

                                            val providerIdToken = OtpData.providerIdToken
                                            val photoUrl = OtpData.providerPhotoUrl







                                            if (!providerIdToken.isNullOrBlank() &&
                                                !photoUrl.isNullOrBlank()
                                            ) {
                                                try {
                                                    val avatarResponse =
                                                        RetrofitClient.api.updateAvatar(
                                                            token = "Bearer $providerIdToken",
                                                            role = loginResult.role ?: role,
                                                            request = AvatarRequest(
                                                                avatarUrl = photoUrl
                                                            )
                                                        )



                                                    if (!avatarResponse.isSuccessful) {

                                                    }

                                                } catch (e: Exception) {

                                                }

                                            } else {

                                            }
                                        }
                                        isLoading = false



                                        OtpData.clear()
                                        onLoginSuccess(loginResult)

                                    } else {
                                        isLoading = false

                                        Toast.makeText(
                                            context,
                                            "Backend không trả về thông tin tài khoản",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                } else {
                                    isLoading = false

                                    if (
                                        provider != "Google" &&
                                        provider != "Facebook" &&
                                        response.code() == 404
                                    ) {
                                        // Số điện thoại đã xác thực Firebase nhưng chưa có tài khoản
                                        onNavigateToSignUp()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Lỗi backend: HTTP ${response.code()}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                            } catch (e: Exception) {

                                isLoading = false

                                Toast.makeText(
                                    context,
                                    "Lỗi kết nối: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->

                        isLoading = false

                        Toast.makeText(
                            context,
                            "Không lấy được Firebase ID Token: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->

                // OTP SAI
                isLoading = false



                Toast.makeText(
                    context,
                    "Mã OTP không đúng hoặc đã hết hạn",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nhập mã OTP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mã xác thực đã được gửi đến số \n$phoneNumber",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        BasicTextField(
            value = otp,
            onValueChange = { text ->
                if (text.length <= 6 && text.all { it.isDigit() } && !isLoading) {
                    otp = text

                    if (otp.length == 6) {
                        isLoading = true
                        keyboardController?.hide()
                        // TODO: Gọi hàm gửi request ở đây
                        // Ví dụ: viewModel.verifyOtp(otp)
                        verifyOtp()
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val char = when {
                            index >= otp.length -> ""
                            else -> otp[index].toString()
                        }

                        val isFocused = otp.length == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (timer > 0) {
            Text(
                text = "Gửi lại mã sau ${timer}s",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            TextButton(
                onClick = { resendOtp() },
                enabled = !isResending
            ) {
                Text(if (isResending) "Đang gửi mã…" else "Gửi lại mã", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Button(
                onClick = {
                    isLoading = true
                    keyboardController?.hide()
                    /* viewModel.verifyOtp(otp) */
                    verifyOtp()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = otp.length == 6
            ) {
                Text("Xác nhận", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
