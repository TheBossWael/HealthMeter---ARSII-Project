package com.example.healthmeter.model

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val age: Int = 0,
    val gender: Gender = Gender.Male,
    val height: Int = 0,
    val weight: Double = 0.0,
    val skinTone: SkinTone = SkinTone.Light
)

enum class Gender {
    Male, Female
}
enum class SkinTone {
    Light, Medium, Dark
}

fun genderFromString(gender: String): Gender {
    return when (gender.lowercase()) {
        "male" -> Gender.Male
        "female" -> Gender.Female
        else -> throw IllegalArgumentException("Invalid gender value")
    }
}

fun skinToneFromString(skinTone: String): SkinTone {
    return when (skinTone.lowercase()) {
        "light" -> SkinTone.Light
        "medium" -> SkinTone.Medium
        "dark" -> SkinTone.Dark
        else -> throw IllegalArgumentException("Invalid skin tone value")
    }
}

