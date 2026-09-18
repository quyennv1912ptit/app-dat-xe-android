package com.example.app_dat_xe

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app_dat_xe.feature.auth.ui.LinkPhoneScreen
import com.example.app_dat_xe.feature.auth.ui.LoginScreen
import com.example.app_dat_xe.feature.auth.ui.OtpVerifyScreen
import com.example.app_dat_xe.feature.auth.ui.SignUpScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController=navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToOtp = { phone ->
                    navController.navigate("otp/$phone")
                },
                onNavigateToLinkPhone = { provider ->
                    navController.navigate("link_phone/$provider")
                }
            )
        }

        composable(
            route = "link_phone/{provider}",
            arguments = listOf(navArgument("provider") { type = NavType.StringType })
            ) {backStackEntry ->
            val provider = backStackEntry.arguments?.getString("provider") ?: "Mạng xã hội"
            LinkPhoneScreen(
                provider = provider,
                onNavigateToOtp = { phone ->
                    navController.navigate("otp/$phone")
                }
            )
        }

        composable(
            route = "otp/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""

            OtpVerifyScreen(phoneNumber = phoneNumber) {
                navController.navigate("signup") {
                    popUpTo("otp/{phoneNumber}") { inclusive = true }
                }
            }
        }

        composable("signup") {
            SignUpScreen(onSignUpSuccess = {
                // Chuyển sang màn hình chính và xóa sạch toàn bộ lịch sử luồng Auth
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("main") {
            MainScreen() // Gọi màn hình vỏ bọc chứa Bottom Nav
        }
    }
}