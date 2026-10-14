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

    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()
    val callbackManager = remember {
        CallbackManager.Factory.create()
    }
    fun sendFacebookTokenToBackend(idToken: String) {



        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response = RetrofitClient.api.loginWithFacebook(
                    LoginRequest(
                        idToken = idToken,
                        role = role
                    )
                )



                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {

                        val result = response.body()



                        if (result == null) {

                            return@withContext
                        }



                        if (result.role == "LINK_PHONE") {



                            OtpData.facebookIdToken = idToken
                            OtpData.providerIdToken = idToken

                            onNavigateToLinkPhone(
                                "Facebook",
                                role
                            )

                        } else {



                            onLoginSuccess(result)
                        }

                    } else {

                        val errorBody =
                            response.errorBody()?.string()





                        Toast.makeText(
                            context,
                            "Facebook login thất bại: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {



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



                    val facebookCredential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    val facebookToken = result.accessToken

                    OtpData.providerPhotoUrl = "https://graph.facebook.com/${facebookToken.userId}/picture?type=large"

                    val request = GraphRequest.newMeRequest(facebookToken) { obj, response ->
                        try {
                            val pictureUrl = obj?.getJSONObject("picture")?.getJSONObject("data")?.getString("url")

                            if (pictureUrl != null) {
                                OtpData.providerPhotoUrl = pictureUrl
                            }
                        } catch (e: Exception) {

                        }
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "id,name,email,picture.type(large)")
                    request.parameters = parameters
                    request.executeAsync()

                    auth.signInWithCredential(facebookCredential)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {



                                auth.currentUser
                                    ?.getIdToken(true)
                                    ?.addOnCompleteListener { tokenTask ->

                                        if (tokenTask.isSuccessful) {

                                            val idToken =
                                                tokenTask.result?.token

                                            if (idToken != null) {



                                                sendFacebookTokenToBackend(idToken)

                                            } else {


                                            }

                                        } else {


                                        }
                                    }

                            } else {

                                val exception = task.exception



                                if (exception is FirebaseAuthUserCollisionException) {



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



                                                        val currentUser =
                                                            auth.currentUser

                                                        if (currentUser == null) {

                                                            return@addOnSuccessListener
                                                        }

                                                        currentUser
                                                            .linkWithCredential(
                                                                facebookCredential
                                                            )
                                                            .addOnSuccessListener {



                                                                currentUser
                                                                    .getIdToken(true)
                                                                    .addOnSuccessListener { tokenResult ->

                                                                        val idToken =
                                                                            tokenResult.token

                                                                        if (idToken != null) {



                                                                            sendFacebookTokenToBackend(
                                                                                idToken
                                                                            )
                                                                        }
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->



                                                                Toast.makeText(
                                                                    context,
                                                                    "Liên kết Facebook thất bại: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->



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


                }

                withContext(Dispatchers.Main) {

                    if (result.role == "LINK_PHONE") {
                        OtpData.googleIdToken = idToken
                        OtpData.providerIdToken = idToken
                        OtpData.providerPhotoUrl = photoUrl

                        onNavigateToLinkPhone("Google", role)
                    } else {



                        onLoginSuccess(result)
                    }
                }

            } catch (e: Exception) {



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



                        auth.currentUser
                            ?.getIdToken(true)
                            ?.addOnSuccessListener { tokenResult ->

                                val firebaseIdToken =
                                    tokenResult.token

                                if (firebaseIdToken != null) {



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



                                Toast.makeText(
                                    context,
                                    "Lấy Firebase ID Token thất bại: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->



                        Toast.makeText(
                            context,
                            "Firebase Google login thất bại: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } else {



                Toast.makeText(
                    context,
                    "Không nhận được Google ID Token",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {







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
                        OtpData.forceResendingToken = token
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
