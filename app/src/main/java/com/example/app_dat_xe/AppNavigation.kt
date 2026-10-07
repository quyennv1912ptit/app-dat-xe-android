package com.example.app_dat_xe

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app_dat_xe.data.remote.LoginResponse
import com.example.app_dat_xe.feature.auth.ui.LinkPhoneScreen
import com.example.app_dat_xe.feature.auth.ui.LoginScreen
import com.example.app_dat_xe.feature.auth.ui.OtpVerifyScreen
import com.example.app_dat_xe.feature.auth.ui.SignUpScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val prefs = remember {
        context.getSharedPreferences(
            "app_dat_xe",
            android.content.Context.MODE_PRIVATE
        )
    }

    val savedRole = prefs.getString("role", null)

    var loginResponse by remember {
        mutableStateOf(
            if (auth.currentUser != null && savedRole != null) {
                LoginResponse(
                    id = prefs.getString("id", null)?.toLongOrNull(),
                    uid = auth.currentUser!!.uid,
                    email = prefs.getString("email", null),
                    phoneNumber = prefs.getString("phoneNumber", null),
                    fullName = prefs.getString("fullName", null),
                    role = savedRole
                )
            } else {
                null
            }
        )
    }

    fun saveLogin(response: LoginResponse) {
        prefs.edit()
            .putString("id", response.id?.toString())
            .putString("uid", response.uid)
            .putString("email", response.email)
            .putString("phoneNumber", response.phoneNumber)
            .putString("fullName", response.fullName)
            .putString("role", response.role)
            .apply()

        loginResponse = response
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()

        prefs.edit().clear().apply()

        loginResponse = null

        navController.navigate("login") {
            popUpTo("main") {
                inclusive = true
            }
        }
    }

    val startDestination =
        if (loginResponse != null) "main" else "login"
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("login") {

            LoginScreen(

                // Đăng nhập bằng số điện thoại bình thường
                onNavigateToOtp = { phone, role ->

                    navController.navigate(
                        "otp/$phone/$role/PHONE"
                    )
                },

                // Facebook / Google
                onNavigateToLinkPhone = { provider, role ->
                    navController.navigate("link_phone/$provider/$role")
                },

                // Đăng nhập thành công
                onLoginSuccess = { response ->
                    Log.d("LOGIN_RESPONSE", "response = $response")
                    Log.d("LOGIN_RESPONSE", "id = ${response.id}")
                    Log.d("LOGIN_RESPONSE", "uid = ${response.uid}")
                    Log.d("LOGIN_RESPONSE", "email = ${response.email}")
                    Log.d("LOGIN_RESPONSE", "phone = ${response.phoneNumber}")
                    Log.d("LOGIN_RESPONSE", "name = ${response.fullName}")
                    Log.d("LOGIN_RESPONSE", "role = ${response.role}")

                    saveLogin(response)

                    navController.navigate("main") {

                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = "link_phone/{provider}/{role}",

            arguments = listOf(
                navArgument("provider") {
                    type = NavType.StringType
                },
                navArgument("role") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val provider =
                backStackEntry.arguments
                    ?.getString("provider")
                    ?: "Mạng xã hội"
            val role =
                backStackEntry.arguments
                    ?.getString("role")
                    ?: "CUSTOMER"

            LinkPhoneScreen(
                provider = provider,
                onNavigateToOtp = { phone ->
                    navController.navigate("otp/$phone/$role/$provider")
                },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }


        composable(
            route = "otp/{phoneNumber}/{role}/{provider}",

            arguments = listOf(
                navArgument("phoneNumber") {
                    type = NavType.StringType
                },
                navArgument("role") {
                    type = NavType.StringType
                },
                navArgument("provider") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val phoneNumber =
                backStackEntry.arguments
                    ?.getString("phoneNumber")
                    ?: ""
            val role =
                backStackEntry.arguments
                    ?.getString("role")
                    ?: "CUSTOMER"
            val provider =
                backStackEntry.arguments
                    ?.getString("provider")
                    ?: "PHONE"


            OtpVerifyScreen(
                phoneNumber = phoneNumber,
                role = role,
                provider = provider,

                onNavigateToSignUp = {
                    navController.navigate("signup/$role")
                },

                onLoginSuccess = { response ->

                    saveLogin(response)

                    navController.navigate("main") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = "signup/{role}",
            arguments = listOf(
                navArgument("role") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val role =
                backStackEntry.arguments
                    ?.getString("role")
                    ?: "CUSTOMER"

            SignUpScreen(
                role = role,

                onSignUpSuccess = { response ->

                    saveLogin(response)

                    navController.navigate("main") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("main") {

            MainScreen(
                fullName = loginResponse?.fullName,
                phoneNumber = loginResponse?.phoneNumber,
                email = loginResponse?.email,
                role = loginResponse?.role,
                onLogout = {
                    logout()
                }
            )
        }
    }
}