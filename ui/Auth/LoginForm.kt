package com.example.healthmeter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmeter.Assets.BoxBackgroundLayout
import com.example.healthmeter.Assets.LoadingButtonWithSpinner
import com.example.healthmeter.Assets.WhiteMiddleLogo
import com.example.healthmeter.ui.Auth.InputField
import com.example.healthmeter.ui.Auth.isLoginInputValid



// Login screen with top logo and bottom login form
@Composable
fun LoginForm(
    onLogin: (username: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    // State variables for username, password, and visibility
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // Shape for rounded corners
    val roundedShape = RoundedCornerShape(20.dp)
    // Check if the form is valid
    val isFormValid = isLoginInputValid(email, password)


    BoxBackgroundLayout(
        topContent = {
            WhiteMiddleLogo(modifier = Modifier.size(200.dp))
        },
        bottomContent = {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .background(Color(0xFFF6F8FA))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LoginHeader()

                InputField(
                    // Full name input
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "example@mail.com",
                    shape = roundedShape,
                    leadingIcon = Icons.Outlined.Email,
                )

                PasswordInputField(
                    label = "Password",
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = it },
                    shape = roundedShape,
                    leadingIcon = Icons.TwoTone.Lock
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onForgotPassword) {
                        Text(
                            "Forgot password?",
                            color = Color(0xFF133a94),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                // Button with loading spinner
                LoadingButtonWithSpinner(
                    buttonText = "Login",
                    onSubmit = { onLogin(email, password) },
                    isFormValid = isFormValid
                )


                SignUpPrompt(onNavigateToSignUp)
            }
        }
    )
}

// Text shown above login form
@Composable
fun LoginHeader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Enter your details below to log in to your account.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

// Sign-up link below the login form
@Composable
fun SignUpPrompt(onNavigateToSignUp: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Don't have an account?", fontSize = 14.sp, color = Color.Gray)
        TextButton(onClick = onNavigateToSignUp) {
            Text(
                "Sign up here",
                color = Color(0xFF133a94),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// Standard text input with optional icon
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(top=15.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = shape,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        )
    )
}

// Password field with show/hide icon toggle
@Composable
fun PasswordInputField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    shape: Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    TextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(top=15.dp),
        singleLine = true,
        shape = shape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = {
            val image = if (passwordVisible) Icons.TwoTone.Visibility else Icons.TwoTone.VisibilityOff
                IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password") } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Color(0xFF133A94),
            unfocusedBorderColor = Color.Transparent
        )
    )
}
