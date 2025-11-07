package com.example.healthmeter.ui.Auth

import android.util.Patterns.EMAIL_ADDRESS
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.rotate
import com.example.healthmeter.Assets.LoadingButtonWithSpinner


@Composable
fun ForgotPass(
        backToLogin: () -> Unit,
        onForgetPass: (email: String) -> Unit
    )
{
    var email by remember { mutableStateOf("") }
    // Validate inputs
    val isFormValid = isEmailValid(email)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(horizontal = 24.dp, vertical = 50.dp)
            .imePadding()
    ) {
        // Top-left Back Icon

        IconButton(
            onClick = { backToLogin()}, //navigate back to login
            modifier = Modifier
                .padding(top = 24.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Back",
                tint = Color(0xFF133a94),
                modifier = Modifier
                    .size(28.dp)
                    .rotate(180f) // Make it point left
            )
        }

        // Middle Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 100.dp), // give space under arrow
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Forgot Password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Enter your email and we’ll send you instructions to reset your password.",
                fontSize = 14.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(0.9f)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("example@mail.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }

        // Bottom Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Button with loading spinner
            LoadingButtonWithSpinner(
                buttonText = "Send Reset Link",
                onSubmit = { onForgetPass(email) },
                isFormValid = isFormValid
            )
        }
    }
}
