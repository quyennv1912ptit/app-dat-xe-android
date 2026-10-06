package com.example.app_dat_xe.feature.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.app_dat_xe.R
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
import android.content.Intent
import android.content.pm.PackageManager
import android.credentials.GetCredentialException
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.example.app_dat_xe.data.remote.LoginRequest
import com.example.app_dat_xe.data.remote.LoginResponse
import com.example.app_dat_xe.data.remote.RetrofitClient
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.login.LoginBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.CancellationSignal
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import androidx.compose.ui.res.stringResource
import com.example.app_dat_xe.data.remote.AvatarRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuthUserCollisionException

@Composable
fun LoginScreen(onNavigateToOtp: (String, String) -> Unit
                , onNavigateToLinkPhone: (String, String) -> Unit
                , onLoginSuccess: (LoginResponse) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CUSTOMER") }
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    Log.d("GOOGLE_LOGIN", "webClientId = $webClientId")
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val callbackManager = remember {
        CallbackManager.Factory.create()
    }
    fun sendFacebookTokenToBackend(idToken: String) {

        Log.d(
            "FACEBOOK_LOGIN",
            "4. Bắt đầu gọi Backend"
        )

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = RetrofitClient.api.loginWithFacebook(
                    LoginRequest(
                        idToken = idToken,
                        role = role
                    )
                )

                Log.d(
                    "FACEBOOK_LOGIN",
                    "5. HTTP = ${response.code()}"
                )

                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {

                        val result = response.body()

                        Log.d(
                            "FACEBOOK_LOGIN",
                            "6. RESULT = $result"
                        )

                        if (result == null) {
                            Log.e(
                                "FACEBOOK_LOGIN",
                                "Backend trả body = null"
                            )
                            return@withContext
                        }

                        Log.d(
                            "FACEBOOK_LOGIN",
                            "7. ROLE = ${result.role}"
                        )

                        if (result.role == "LINK_PHONE") {

                            Log.d(
                                "FACEBOOK_LOGIN",
                                "8. Chuyển sang LinkPhoneScreen"
                            )

                            OtpData.facebookIdToken = idToken
                            OtpData.providerIdToken = idToken

                            onNavigateToLinkPhone(
                                "Facebook",
                                role
                            )

                        } else {

                            Log.d(
                                "FACEBOOK_LOGIN",
                                "8. Chuyển vào Main: ${result.role}"
                            )

                            onLoginSuccess(result)
                        }

                    } else {

                        val errorBody =
                            response.errorBody()?.string()

                        Log.e(
                            "FACEBOOK_LOGIN",
                            "Backend HTTP ERROR = ${response.code()}"
                        )

                        Log.e(
                            "FACEBOOK_LOGIN",
                            "ERROR BODY = $errorBody"
                        )

                        Toast.makeText(
                            context,
                            "Facebook login thất bại: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "FACEBOOK_LOGIN",
                    "Gọi backend thất bại",
                    e
                )

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        context,
                        "Facebook lỗi: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    val facebookLauncher = rememberLauncherForActivityResult(
        contract = LoginManager.getInstance()
            .createLogInActivityResultContract(callbackManager)
    ) {
    }
    LaunchedEffect(callbackManager) {
        if (
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_LOCAL_NETWORK),
                1001
            )
        }
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {

                override fun onSuccess(result: LoginResult) {

                    Log.d(
                        "FACEBOOK_LOGIN",
                        "Facebook User ID = ${result.accessToken.userId}"
                    )

                    val facebookCredential =
                        FacebookAuthProvider.getCredential(
                            result.accessToken.token
                        )
                    val facebookToken = result.accessToken

                    val facebookPhotoUrl =
                        "https://graph.facebook.com/${facebookToken.userId}/picture?type=large"

                    OtpData.providerPhotoUrl = facebookPhotoUrl

                    Log.d(
                        "FACEBOOK_LOGIN",
                        "Facebook avatar = $facebookPhotoUrl"
                    )

                    val request = GraphRequest.newMeRequest(
                        facebookToken
                    ) { obj, response ->

                        Log.d(
                            "FACEBOOK_LOGIN",
                            "Facebook profile = $obj"
                        )

                        Log.d(
                            "FACEBOOK_LOGIN",
                            "Facebook email = ${obj?.optString("email")}"
                        )
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "id,name,email,picture.type(large)")
                    request.parameters = parameters
                    request.executeAsync()

                    auth.signInWithCredential(facebookCredential)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                Log.d(
                                    "FACEBOOK_LOGIN",
                                    "1. Firebase Facebook login SUCCESS"
                                )

                                auth.currentUser
                                    ?.getIdToken(true)
                                    ?.addOnCompleteListener { tokenTask ->

                                        if (tokenTask.isSuccessful) {

                                            val idToken =
                                                tokenTask.result?.token

                                            if (idToken != null) {

                                                Log.d(
                                                    "FACEBOOK_LOGIN",
                                                    "2. Firebase ID Token SUCCESS"
                                                )

                                                sendFacebookTokenToBackend(idToken)

                                            } else {

                                                Log.e(
                                                    "FACEBOOK_LOGIN",
                                                    "Firebase ID Token = null"
                                                )
                                            }

                                        } else {

                                            Log.e(
                                                "FACEBOOK_LOGIN",
                                                "Lấy Firebase ID Token FAILED",
                                                tokenTask.exception
                                            )
                                        }
                                    }

                            } else {

                                val exception = task.exception

                                Log.e(
                                    "FACEBOOK_LOGIN",
                                    "Firebase Facebook login FAILED",
                                    exception
                                )

                                if (exception is FirebaseAuthUserCollisionException) {

                                    Log.d(
                                        "FACEBOOK_LOGIN",
                                        "Email đã tồn tại → cần đăng nhập Google trước"
                                    )

                                    CoroutineScope(Dispatchers.Main).launch {

                                        try {

                                            val credentialManager =
                                                CredentialManager.create(context)

                                            val googleOption =
                                                GetSignInWithGoogleOption.Builder(
                                                    serverClientId = webClientId
                                                ).build()

                                            val request =
                                                GetCredentialRequest.Builder()
                                                    .addCredentialOption(googleOption)
                                                    .build()

                                            val googleResult =
                                                credentialManager.getCredential(
                                                    context = context,
                                                    request = request
                                                )

                                            val googleCredentialData =
                                                googleResult.credential

                                            if (
                                                googleCredentialData is CustomCredential &&
                                                googleCredentialData.type ==
                                                GoogleIdTokenCredential
                                                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            ) {

                                                val googleCredential =
                                                    GoogleIdTokenCredential.createFrom(
                                                        googleCredentialData.data
                                                    )

                                                val googleIdToken =
                                                    googleCredential.idToken

                                                val firebaseGoogleCredential =
                                                    GoogleAuthProvider.getCredential(
                                                        googleIdToken,
                                                        null
                                                    )

                                                auth.signInWithCredential(
                                                    firebaseGoogleCredential
                                                )
                                                    .addOnSuccessListener {

                                                        Log.d(
                                                            "FACEBOOK_LOGIN",
                                                            "Google Firebase login SUCCESS"
                                                        )

                                                        val currentUser =
                                                            auth.currentUser

                                                        if (currentUser == null) {
                                                            Log.e(
                                                                "FACEBOOK_LOGIN",
                                                                "currentUser = null"
                                                            )
                                                            return@addOnSuccessListener
                                                        }

                                                        currentUser
                                                            .linkWithCredential(
                                                                facebookCredential
                                                            )
                                                            .addOnSuccessListener {

                                                                Log.d(
                                                                    "FACEBOOK_LOGIN",
                                                                    "Facebook link SUCCESS"
                                                                )

                                                                currentUser
                                                                    .getIdToken(true)
                                                                    .addOnSuccessListener { tokenResult ->

                                                                        val idToken =
                                                                            tokenResult.token

                                                                        if (idToken != null) {

                                                                            Log.d(
                                                                                "FACEBOOK_LOGIN",
                                                                                "Firebase ID Token SUCCESS"
                                                                            )

                                                                            sendFacebookTokenToBackend(
                                                                                idToken
                                                                            )
                                                                        }
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->

                                                                Log.e(
                                                                    "FACEBOOK_LOGIN",
                                                                    "Facebook link FAILED",
                                                                    e
                                                                )

                                                                Toast.makeText(
                                                                    context,
                                                                    "Liên kết Facebook thất bại: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->

                                                        Log.e(
                                                            "FACEBOOK_LOGIN",
                                                            "Google Firebase login FAILED",
                                                            e
                                                        )

                                                        Toast.makeText(
                                                            context,
                                                            "Google login thất bại: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }

                                            } else {

                                                Toast.makeText(
                                                    context,
                                                    "Không lấy được tài khoản Google",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }

                                        } catch (e: Exception) {

                                            Log.e(
                                                "FACEBOOK_LOGIN",
                                                "Google linking ERROR",
                                                e
                                            )

                                            Toast.makeText(
                                                context,
                                                "Không thể liên kết Facebook: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Facebook login thất bại: ${exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                }

                override fun onCancel() {
                    Toast.makeText(
                        context,
                        "Đăng nhập Facebook bị hủy",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(
                        context,
                        "Facebook login lỗi: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.e(
                        "FACEBOOK_LOGIN",
                        "Facebook callback lỗi",
                        error
                    )
                }
            }
        )
    }
    DisposableEffect(callbackManager) {
        onDispose {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }
    fun loginWithFacebook() {
        // Đăng xuất phiên Firebase hiện tại
        auth.signOut()

        // Đăng xuất phiên Facebook hiện tại
        LoginManager.getInstance().logOut()

        LoginManager.getInstance().setLoginBehavior(
            LoginBehavior.WEB_ONLY
        )

        facebookLauncher.launch(
            listOf(
                "public_profile",
                "email"
            )
        )
    }
    fun sendGoogleTokenToBackend(idToken: String, photoUrl: String?) {

        Log.d(
            "GOOGLE_LOGIN",
            "4. Bắt đầu gọi Backend với Firebase ID Token"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.loginWithGoogle(
                    LoginRequest(
                        idToken = idToken,
                        role = role
                    )
                )

                if (!response.isSuccessful) {
                    throw Exception(
                        "Đăng nhập Google thất bại: ${response.code()}"
                    )
                }

                val result = response.body()
                    ?: throw Exception("Backend không trả về dữ liệu")

                Log.d(
                    "GOOGLE_LOGIN",
                    "BACKEND RESULT = $result"
                )

                // Chỉ lưu avatar khi tài khoản đã đăng nhập hoàn tất
                if (
                    result.role != "LINK_PHONE" &&
                    !photoUrl.isNullOrBlank()
                ) {
                    val avatarResponse = RetrofitClient.api.updateAvatar(
                        token = "Bearer $idToken",
                        role = result.role ?: role,
                        request = AvatarRequest(
                            avatarUrl = photoUrl
                        )
                    )

                    Log.d(
                        "PROVIDER_AVATAR",
                        "Cập nhật avatar HTTP = ${avatarResponse.code()}"
                    )
                }

                withContext(Dispatchers.Main) {

                    if (result.role == "LINK_PHONE") {
                        OtpData.googleIdToken = idToken
                        OtpData.providerIdToken = idToken
                        OtpData.providerPhotoUrl = photoUrl

                        onNavigateToLinkPhone("Google", role)
                    } else {

                        Log.d(
                            "GOOGLE_LOGIN",
                            "RESULT GMAIL = ${result.email}"
                        )

                        onLoginSuccess(result)
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "GOOGLE_LOGIN",
                    "Gọi backend thất bại",
                    e
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Google login thất bại: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    suspend fun loginWithGoogle() {
        try {
            val credentialManager = CredentialManager.create(context)

            val googleOption =
                GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            Log.d(
                "GOOGLE_LOGIN",
                "Đang mở Google Sign-In"
            )

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential

            if (
                credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        credential.data
                    )

                val googleIdToken =
                    googleCredential.idToken

                Log.d(
                    "GOOGLE_LOGIN",
                    "1. Lấy Google ID Token SUCCESS"
                )

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleIdToken,
                        null
                    )

                auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener {

                        val googlePhotoUrl = auth.currentUser
                            ?.photoUrl
                            ?.toString()

                        Log.d(
                            "GOOGLE_LOGIN",
                            "2. Firebase Google login SUCCESS"
                        )

                        auth.currentUser
                            ?.getIdToken(true)
                            ?.addOnSuccessListener { tokenResult ->

                                val firebaseIdToken =
                                    tokenResult.token

                                if (firebaseIdToken != null) {

                                    Log.d(
                                        "GOOGLE_LOGIN",
                                        "3. Firebase ID Token SUCCESS"
                                    )

                                    sendGoogleTokenToBackend(
                                        firebaseIdToken, googlePhotoUrl
                                    )

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Không lấy được Firebase ID Token",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            ?.addOnFailureListener { e ->

                                Log.e(
                                    "GOOGLE_LOGIN",
                                    "Lấy Firebase ID Token FAILED",
                                    e
                                )

                                Toast.makeText(
                                    context,
                                    "Lấy Firebase ID Token thất bại: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->

                        Log.e(
                            "GOOGLE_LOGIN",
                            "Firebase Google login FAILED",
                            e
                        )

                        Toast.makeText(
                            context,
                            "Firebase Google login thất bại: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {

                Log.e(
                    "GOOGLE_LOGIN",
                    "Credential không phải Google ID Token"
                )

                Toast.makeText(
                    context,
                    "Không nhận được Google ID Token",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Log.e(
                "GOOGLE_LOGIN",
                "Google login lỗi: ${e.javaClass.name}"
            )

            Log.e(
                "GOOGLE_LOGIN",
                "message = ${e.message}"
            )

            Log.e(
                "GOOGLE_LOGIN",
                "cause = ${e.cause}"
            )

            Toast.makeText(
                context,
                "Google lỗi: ${e.javaClass.simpleName}\n${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    fun sendOtp() {
        if (phoneNumber.isBlank()) {
            Toast.makeText(
                context,
                "Vui lòng nhập số điện thoại",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

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
                        // Firebase tự xác thực trong một số trường hợp
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
                        onNavigateToOtp(formattedPhone, role)
                    }
                }
            )
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.padding(24.dp)) {

        Text(
            text = "Chào mừng bạn",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { role = "CUSTOMER" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (role == "CUSTOMER")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Khách")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { role = "DRIVER" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (role == "DRIVER")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Lái xe")
            }
        }


        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text(text = "Nhập số điện thoại") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                sendOtp()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(text = "Tiếp tục", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    loginWithGoogle()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(text = "Tiếp tục với Google")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { loginWithFacebook() },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.facebook),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Tiếp tục với Facebook")
        }
    }
}
