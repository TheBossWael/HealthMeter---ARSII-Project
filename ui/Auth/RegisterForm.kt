package com.example.healthmeter.ui.Auth

import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.healthmeter.Assets.LoadingButtonWithSpinner


// Registration form with user profile fields and validation
@Composable
fun RegisterForm(
    onRegister: (userData : List<String>) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // State variables for form inputs
    // Using mutableStateOf to hold the state of each input field
    var Username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedSkinTone by remember { mutableStateOf("") }
    var genderExpanded by remember { mutableStateOf(false) }
    var skinToneExpanded by remember { mutableStateOf(false) }
    // Shape for rounded corners
    val roundedShape = RoundedCornerShape(20.dp)

    // Collect all New user data into a list
    val userData = listOf(Username, email, password, age, selectedGender, height, weight, selectedSkinTone)
    // Validate inputs from the view model
    val isFormValid = isRegisterInputValid(userData)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(35.dp))
        RegisterProfileHeader() // Header text
        Spacer(modifier = Modifier.height(5.dp))

        InputField( // Full name input
            label = "Username",
            value = Username,
            onValueChange = { Username = it },
            placeholder = "Enter your Username",
            shape = roundedShape,
            leadingIcon = Icons.Outlined.Person
        )

        InputField( // Full name input
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "example@mail.com",
            shape = roundedShape,
            leadingIcon = Icons.Outlined.Email,
        )

        PasswordInputField( // Password input with visibility toggle
            label = "Password",
            password = password,
            onPasswordChange = { password = it },
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Lock
        )

        NumberInputField( // Age input (only digits)
            label = "Age",
            value = age,
            onValueChange = { newValue -> if (newValue.all { it.isDigit() }) age = newValue.take(3) },
            placeholder = "Enter your age",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Cake
        )

        DropdownField( // Gender selection dropdown
            label = "Gender",
            options = listOf("Male", "Female"),
            selectedOption = selectedGender,
            onOptionSelected = { selectedGender = it },
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Wc
        )

        NumberInputField( // Height input in cm
            label = "Height (cm)",
            value = height,
            onValueChange = { newValue -> if (newValue.all { it.isDigit() }) height = newValue.take(3) },
            placeholder = "Enter your height",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Height
        )

        NumberInputField( // Weight input in kg (allows decimals)
            label = "Weight (kg)",
            value = weight,
            onValueChange = { newValue -> if (newValue.all { it.isDigit() || it == '.' }) weight = newValue.take(5) },
            placeholder = "Enter your weight",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.FitnessCenter
        )

        DropdownField( // Skin tone selection dropdown
            label = "Skin Tone",
            options = listOf("Light", "Medium", "Dark"),
            selectedOption = selectedSkinTone,
            onOptionSelected = { selectedSkinTone = it },
            expanded = skinToneExpanded,
            onExpandedChange = { skinToneExpanded = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Face
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button with loading spinner
        LoadingButtonWithSpinner(
            buttonText = "Continue",
            onSubmit = { onRegister(userData) },
            isFormValid = isFormValid
        )


        Spacer(modifier = Modifier.height(15.dp))
        // Login prompt
        SignInPrompt(onNavigateToLogin)
    }
}

// Header text for the registration form
@Composable
fun RegisterProfileHeader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                "Create a new account to get started and enjoy seamless access to our features.",
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

// Generic input field with optional leading icon
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().padding(top=15.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = shape,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent
    )
    )
}

// Password input field with visibility toggle
@Composable
fun PasswordInputField(
    label: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    TextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(top=15.dp),
        singleLine = true,
        shape = shape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = {
            val image = if (passwordVisible) Icons.TwoTone.Visibility else Icons.TwoTone.VisibilityOff
            IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

// Input field for numeric values like age, height, weight
@Composable
fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    InputField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = shape,
        leadingIcon = leadingIcon,

    )
}

// Dropdown menu field for selecting an option from a list
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    leadingIcon: ImageVector? = null
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().padding(top=15.dp),
            singleLine = true,
            shape = shape,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// Login redirect prompt at bottom of registration screen
@Composable
fun SignInPrompt(onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Already have an account?", fontSize = 14.sp, color = Color.Gray)
        TextButton(
            onClick = onNavigateToLogin,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text(
                "Sign in here",
                color = Color(0xFF133a94),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
