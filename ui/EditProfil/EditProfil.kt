package com.example.healthmeter.ui.EditProfil

import GetCurrentUser
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.twotone.Cake
import androidx.compose.material.icons.twotone.Face
import androidx.compose.material.icons.twotone.FitnessCenter
import androidx.compose.material.icons.twotone.Height
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Wc
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthmeter.Assets.LoadingButtonWithSpinner
import com.example.healthmeter.ui.Auth.DropdownField
import com.example.healthmeter.ui.Auth.InputField
import com.example.healthmeter.ui.Auth.NumberInputField
import com.example.healthmeter.ui.Auth.PasswordInputField
import com.example.healthmeter.model.Gender
import com.example.healthmeter.model.SkinTone
import java.util.logging.Handler
import kotlin.String


//prefill user info
//get the fields data from this page
    // it can be only either new info or old info
// so if the user doesn't change a field, it will remain with old user data
// check old password // fun from auth_repository
//if old password is correct :
    //update the user info
    //if password changed run changePass(newPass) // fun from auth_repository
    //if email changed, verify the new email
@Composable
fun EditProfil(
    modifier: Modifier = Modifier,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var selectedSkinTone by remember { mutableStateOf("") }

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    var genderExpanded by remember { mutableStateOf(false) }
    var skinToneExpanded by remember { mutableStateOf(false) }

    val roundedShape = RoundedCornerShape(20.dp)
    val genderOptions = Gender.values().map { it.name }
    val skinToneOptions = SkinTone.values().map { it.name }

    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Store original values for comparison
    var originalUsername by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var originalAge by remember { mutableStateOf("") }
    var originalHeight by remember { mutableStateOf("") }
    var originalWeight by remember { mutableStateOf("") }
    var originalGender by remember { mutableStateOf("") }
    var originalSkinTone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        GetCurrentUser { user ->
            user?.let { u ->
                username = u.username
                email = u.email
                originalUsername = u.username
                originalEmail = u.email
                age = u.age.toString()
                originalAge = u.age.toString()
                height = u.height.toString()
                originalHeight = u.height.toString()
                weight = u.weight.toString()
                originalWeight = u.weight.toString()
                selectedGender = u.gender.name
                originalGender = u.gender.name
                selectedSkinTone = u.skinTone.name
                originalSkinTone = u.skinTone.name
            }
            loading = false
        }
    }

    if (loading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Edit Profile")
        Spacer(modifier = Modifier.height(16.dp))

        InputField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "example@mail.com",
            shape = roundedShape,
            leadingIcon = Icons.Outlined.Email
        )

        PasswordInputField(
            label = "New Password",
            password = newPassword,
            onPasswordChange = { newPassword = it },
            passwordVisible = newPasswordVisible,
            onPasswordVisibilityChange = { newPasswordVisible = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Lock
        )

        NumberInputField(
            label = "Age",
            value = age,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) age = newValue.take(3)
            },
            placeholder = "Enter your age",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Cake
        )

        DropdownField(
            label = "Gender",
            options = genderOptions,
            selectedOption = selectedGender,
            onOptionSelected = { selectedGender = it },
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Wc
        )

        NumberInputField(
            label = "Height (cm)",
            value = height,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) height = newValue.take(4)
            },
            placeholder = "Enter your height in cm",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Height
        )

        NumberInputField(
            label = "Weight (kg)",
            value = weight,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' } || newValue.isEmpty()) weight = newValue.take(6)
            },
            placeholder = "Enter your weight (e.g. 72.5)",
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.FitnessCenter
        )

        DropdownField(
            label = "Skin Tone",
            options = skinToneOptions,
            selectedOption = selectedSkinTone,
            onOptionSelected = { selectedSkinTone = it },
            expanded = skinToneExpanded,
            onExpandedChange = { skinToneExpanded = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Face
        )

        PasswordInputField(
            label = "Old Password",
            password = oldPassword,
            onPasswordChange = { oldPassword = it },
            passwordVisible = oldPasswordVisible,
            onPasswordVisibilityChange = { oldPasswordVisible = it },
            shape = roundedShape,
            leadingIcon = Icons.TwoTone.Lock
        )

        Spacer(modifier = Modifier.height(20.dp))

        LoadingButtonWithSpinner(
            buttonText = "Save",
            onSubmit = {
                submitEditProfile(
                    context,
                    username,
                    email,
                    age,
                    height,
                    weight,
                    selectedGender,
                    selectedSkinTone,
                    oldPassword,
                    newPassword,
                    originalEmail
                )
            },
            isFormValid = isUpdateProfileInputValid(
                email = email,
                password = oldPassword,
                age = age,
                gender = selectedGender,
                height = height,
                weight = weight,
                skinTone = selectedSkinTone,
                originalEmail = originalEmail,
                originalAge = originalAge,
                originalGender = originalGender,
                originalHeight = originalHeight,
                originalWeight = originalWeight,
                originalSkinTone = originalSkinTone
            )
        )
    }
}
