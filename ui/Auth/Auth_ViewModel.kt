package com.example.healthmeter.ui.Auth
import android.content.Context
import android.util.Patterns
import androidx.navigation.NavHostController
import checkUserExists
import com.example.healthmeter.Assets.Screen
import com.example.healthmeter.Assets.showToast
import com.example.healthmeter.model.User
import com.example.healthmeter.model.genderFromString
import com.example.healthmeter.model.skinToneFromString
import forgetPassword
import loginUser
import registerUser
import kotlin.text.trim


// Validate email format
fun isEmailValid(email: String): Boolean
{
    val trimmedEmail = email.trim()
    return Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
}


// Validate register input
fun isRegisterInputValid(data: List<String>): Boolean
{

    val username = data[0].trim()
    val email = data[1].trim()
    val password = data[2].trim()
    val age = data[3].trim()
    val gender = data[4].trim()
    val height = data[5].trim()
    val weight = data[6].trim()
    val skinTone = data[7].trim()

    return username.isNotEmpty() &&
            email.isNotEmpty() && isEmailValid(email) &&
            password.isNotEmpty() &&
            age.isNotEmpty() && age.toIntOrNull()?.let { it > 0 } == true &&
            gender.isNotEmpty() &&
            height.isNotEmpty() && height.toIntOrNull()?.let { it > 0 } == true &&
            weight.isNotEmpty() && weight.toDoubleOrNull()?.let { it > 0.0 } == true &&
            skinTone.isNotEmpty()
}


//validate login input
fun isLoginInputValid(email: String, password: String, ): Boolean
{
    return  isEmailValid(email) &&
            password.trim().isNotEmpty()
}


fun onForgetPass(email : String, context: Context)
{
    //handle forget password logic with firebase api
    forgetPassword(email, context)
}


//onLogin :
// Login the user
// Navigate to Home page
// show result message
fun onLogin(
    email: String,
    password: String,
    context: Context,
    navController: NavHostController,
    onSuccess: (() -> Unit)? = null,
    onFailure: ((Exception?) -> Unit)? = null
) {
    // Delegate to the auth repo's loginUser which now supports callbacks.
    // If onSuccess/onFailure are null, loginUser will run its legacy navigation fallback.
    loginUser(email, password, context, navController,
        onSuccess = {
            onSuccess?.invoke()
        },
        onFailure = { e ->
            onFailure?.invoke(e)
        }
    )
}


fun onRegister(data: List<String>, context: Context, navController: NavHostController)
{
    val username = data[0].trim().lowercase()

    checkUserExists(username) { exists ->
        if (exists) {
            showToast(context, "Username already exists!")
            return@checkUserExists
        }

        // Create the user object
        val user = User(
            username = username,
            email = data[1].trim(),
            password = data[2].trim(),
            age = data[3].trim().toInt(),
            gender = genderFromString(data[4].trim()),
            height = data[5].trim().toInt(),
            weight = data[6].trim().toDouble(),
            skinTone = skinToneFromString(data[7].trim())
        )

        // Register the user asynchronously
        // Navigate to login page AFTER registration success inside this function
        registerUser(user, context,navController)

    }
}
