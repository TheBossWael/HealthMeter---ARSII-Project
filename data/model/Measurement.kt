package com.example.healthmeter.data.model

import androidx.compose.ui.graphics.Color


data class Measurement(
    val bpm: Int = 0,                   // Heart rate in beats per minute
    val spo2: Int = 0,                  // Oxygen saturation percentage
    val condition: String = "",          // Health condition description
    val timestamp: Long = System.currentTimeMillis(), // Unix time for easy sorting
    val formattedTime: String = "",     // Human-readable time (e.g. "Today at 03:06 am")
)

fun determineCondition(bpm: Int, spo2: Int): String {
    return when {
        spo2 < 89 || bpm < 40 || bpm > 120 -> "Dangerous"
        spo2 in 90..92 || bpm in 40..49 || bpm in 101..120 -> "Bad"
        spo2 in 90..95 && bpm in 60..100 -> "Good"
        spo2 > 95 && bpm in 60..100 -> "Very Good"
        else -> "Unknown"
    }



}
fun getConditionColor(condition: String): Color {
    return when (condition.lowercase()) {
        "very good" -> Color(0xFF285602)  // Green
        "good" -> Color(0xFF41A903)
        "bad" -> Color(0xFFFF9100)
        "dangerous" -> Color(0xFFD50000)
        else -> Color(0xFFB0BEC5)
    }
}