package com.example.healthmeter.Assets

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.healthmeter.HeartbeatWebViewScreen
import com.example.healthmeter.LoginForm
import com.example.healthmeter.data.model.BottomNavigation
import com.example.healthmeter.data.model.navigationItems
import com.example.healthmeter.ui.Auth.ForgotPass
import com.example.healthmeter.ui.Welcoming_ui.SplashScreen
import com.example.healthmeter.ui.Welcoming_ui.WelcomePage
import com.example.healthmeter.ui.Auth.RegisterForm
import com.example.healthmeter.ui.Auth.onForgetPass
import com.example.healthmeter.ui.Auth.onLogin
import com.example.healthmeter.ui.Auth.onRegister
import com.example.healthmeter.ui.BodyHealthCalculator.BodyHealthScreen
import com.example.healthmeter.ui.EditProfil.EditProfil
import com.example.healthmeter.ui.HealthHistory.HealthHistory
import com.example.healthmeter.ui.HealthHistory.MeasurementHistoryScreenWrapper
import com.example.healthmeter.ui.Home.HomeScreen
import com.example.healthmeter.ui.InstructionsForCamScan.InstructionsMeasurementScreen
// import com.example.healthmeter.ui.rppg.RppgScreen
import logoutUser

// This file defines the navigation structure for the app using Jetpack Compose's Navigation component.
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPass : Screen("ForgotPass")
    object HomeScreen : Screen("home_screen")
    object EditProfil : Screen("EditProfil")
    object HealthHistory : Screen("HealthHistory")
    object RppgScreen : Screen("Rppg_screen")

    object BodyHealthScreen : Screen("BodyHealthScreen")
    object InstructionsScreen : Screen("InstructionsScreen")
}


@Composable
fun AuthNavGraph(
    navController: NavHostController,
    // callback to notify caller (MainActivity) that login succeeded and graphs should switch
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current  // Get context once here
    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Welcome.route) {
            WelcomePage(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
        composable(Screen.ForgotPass.route) {
            ForgotPass(
                backToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onForgetPass = { email ->
                    onForgetPass(email, context)
                }
            )
        }
        composable(Screen.Login.route) {
            LoginForm(
                onLogin = { email, password ->
                    // Use the modified onLogin that accepts an onSuccess callback.
                    // when login succeeds it will call onLoginSuccess() so MainActivity can switch graphs.
                    onLogin(
                        email,
                        password,
                        context,
                        navController,
                        onSuccess = {
                            onLoginSuccess()
                        },
                        onFailure = { _ ->
                            // Optional: keep behavior or show additional UI; keep empty to minimize changes
                        }
                    )
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPassword = {
                    navController.navigate(Screen.ForgotPass.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterForm(
                onRegister = { userData ->
                    // keep register behavior unchanged (no callback added here per your request)
                    onRegister(userData, context, navController)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun MainNavGraph(
    navController: NavHostController,
    // callback to notify caller (MainActivity) that logout completed and graphs should switch
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            BottomNavigation(navController, navigationItems = navigationItems)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HomeScreen.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.HomeScreen.route) {
                HomeScreen(
                    logout = {
                        // logoutUser that accepts onComplete callback.
                        // After signOut, call onLogout so MainActivity can switch graphs.
                        logoutUser(navController) {
                            onLogout()
                        }
                    },
                    onNavigateToRppgScreen={
                        navController.navigate(Screen.InstructionsScreen.route)
                    }

                )
            }
            composable(Screen.EditProfil.route) {
                EditProfil()
            }
            composable(Screen.HealthHistory.route) {
                MeasurementHistoryScreenWrapper()
            }
            composable(Screen.RppgScreen.route) {
              /*  LaunchedEffect(Unit) {
                    context.startActivity(Intent(context, RppgScreen::class.java))
                    navController.popBackStack() // pop so you don’t get stuck on a blank screen
                } */
                HeartbeatWebViewScreen(navController)
            }
            composable(Screen.BodyHealthScreen.route) {
                BodyHealthScreen()
            }
            composable(Screen.InstructionsScreen.route) {
                InstructionsMeasurementScreen(
                navController = navController,
                onCancel = { navController.navigate(Screen.HomeScreen.route) },
                onNext = { navController.navigate(Screen.RppgScreen.route) }
                )
            }

        }
    }
}
