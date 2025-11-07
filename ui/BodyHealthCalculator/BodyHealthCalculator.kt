
package com.example.healthmeter.ui.BodyHealthCalculator

import kotlin.math.log10


object
BodyHealthCalculator {

    fun calculateBMI(weightKg: Double, heightCm: Double): Double {
        val heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    fun calculateIdealWeight(heightCm: Double, gender: String): Double {
        return if (gender.lowercase() == "male") {
            50 + 0.9 * (heightCm - 152)
        } else {
            45.5 + 0.9 * (heightCm - 152)
        }
    }

    fun calculateDailyCalories(data: UserHealthData): Double {
        val bmr = if (data.gender.lowercase() == "male") {
            10 * data.weightKg + 6.25 * data.heightCm - 5 * data.age + 5
        } else {
            10 * data.weightKg + 6.25 * data.heightCm - 5 * data.age - 161
        }
        return bmr * data.activityLevel
    }
}


