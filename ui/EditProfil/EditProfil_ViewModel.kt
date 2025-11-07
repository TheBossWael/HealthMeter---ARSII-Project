package com.example.healthmeter.ui.EditProfil

import checkIfPasswordCorrect
import com.example.healthmeter.Assets.showToast
import com.example.healthmeter.model.Gender
import com.example.healthmeter.model.SkinTone
import com.example.healthmeter.model.User
import updateUser
import android.content.Context
import changePass
import com.example.healthmeter.ui.Auth.isEmailValid
import verifyEmailChange

fun submitEditProfile(
    context: Context,
    username: String,
    email: String,
    age: String,
    height: String,
    weight: String,
    selectedGender: String,
    selectedSkinTone: String,
    oldPassword: String,
    newPassword: String,
    currentEmail: String // pass the original email
) {
    checkIfPasswordCorrect(oldPassword) { correct ->
        if (!correct) {
            showToast(context, "Old password is incorrect")
            return@checkIfPasswordCorrect
        }

        // Use old email in database until verification
        val user = User(
            username = username,
            email = currentEmail, // keep old email for now
            age = age.toInt(),
            gender = Gender.valueOf(selectedGender),
            height = height.toInt(),
            weight = weight.toDouble(),
            skinTone = SkinTone.valueOf(selectedSkinTone)
        )

        // Update other user fields in database (email stays old)
        updateUser(user, context)

        // Update password if changed
        if (newPassword.isNotEmpty()) {
            changePass(newPassword, context)
        }

        // Update email in Firebase Auth after verification
        if (email.trim() != currentEmail) {
            verifyEmailChange(email, user, context)
        }
    }
}


// Validate  input
fun isUpdateProfileInputValid(
    email: String,
    password: String,
    age: String,
    gender: String,
    height: String,
    weight: String,
    skinTone: String,
    originalEmail: String,
    originalAge: String,
    originalGender: String,
    originalHeight: String,
    originalWeight: String,
    originalSkinTone: String
): Boolean {
    // Check all validation rules
    val valid = email.isNotEmpty() && isEmailValid(email) &&
            password.isNotEmpty() &&
            age.isNotEmpty() && age.toIntOrNull()?.let { it > 0 } == true &&
            gender.isNotEmpty() &&
            height.isNotEmpty() && height.toIntOrNull()?.let { it > 0 } == true &&
            weight.isNotEmpty() && weight.toDoubleOrNull()?.let { it > 0.0 } == true &&
            skinTone.isNotEmpty()

    if (!valid) return false

    // Return false if nothing changed
    val changed = email != originalEmail ||
            age != originalAge ||
            gender != originalGender ||
            height != originalHeight ||
            weight != originalWeight ||
            skinTone != originalSkinTone

    return changed
}

