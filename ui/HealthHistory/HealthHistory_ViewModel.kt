package com.example.healthmeter.ui.HealthHistory

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.healthmeter.data.model.Measurement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
fun groupMeasurementsByDate(measurements: List<Measurement>): Map<String, List<Measurement>> {
    val today = LocalDate.now()
    val grouped = mutableMapOf<String, MutableList<Measurement>>()

    for (m in measurements.sortedByDescending { it.timestamp }) {
        val date = Instant.ofEpochMilli(m.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val groupKey = when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date.isAfter(today.minusWeeks(1)) -> "Last week"
            else -> date.toString()
        }

        grouped.getOrPut(groupKey) { mutableListOf() }.add(m)
    }

    return grouped
}
