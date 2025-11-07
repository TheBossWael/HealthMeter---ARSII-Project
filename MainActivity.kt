package com.example.healthmeter

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import checkUserLoggedIn
import com.example.healthmeter.Assets.AuthNavGraph
import com.example.healthmeter.Assets.MainNavGraph
import com.example.healthmeter.ui.theme.HealthMeterTheme
import com.example.healthmeter.Assets.Screen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMeterTheme {
                val navController = rememberNavController()

                // Make login state reactive so the UI switches NavHosts when it changes.
                var isLoggedIn by remember { mutableStateOf(checkUserLoggedIn()) }
                if (isLoggedIn) {
                    // Main graph — provide an onLogout callback so the navigation can switch back to Auth graph.
                    MainNavGraph(
                        navController = navController,
                        onLogout = {
                            // update state to show AuthNavGraph
                            isLoggedIn = false
                        }
                    )
                } else {
                    // Auth graph — provide an onLoginSuccess callback so the navigation can switch to Main graph.
                    AuthNavGraph(
                        navController = navController,
                        onLoginSuccess = {
                            // update state to show MainNavGraph
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}
