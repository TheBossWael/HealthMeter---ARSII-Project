package com.example.healthmeter.ui.HealthHistory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmeter.Assets.BoxBackgroundLayout
import com.example.healthmeter.data.model.Measurement
import com.example.healthmeter.data.model.getConditionColor
import com.example.healthmeter.data.repository.getAllMeasurements
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MeasurementHistoryScreenWrapper() {
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    BoxBackgroundLayout(

        topContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 24.dp, bottom = 32.dp)
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Measurements History",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp,
                            fontSize = 42.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Review your past heart rate measurements below",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    )
                }
            }
        },

        bottomContent = {
            LaunchedEffect(Unit) {
                getAllMeasurements(
                    onSuccess = {
                        measurements = it.map { (_, m) -> m }
                        isLoading = false
                    },
                    onFailure = {
                        isLoading = false
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HealthHistory(measurements = measurements)
            }
        }
    )
}

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun HealthHistory(
    modifier: Modifier = Modifier,
    measurements: List<Measurement>
) {
    val grouped = groupMeasurementsByDate(measurements)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        grouped.forEach { (dateGroup, list) ->
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = dateGroup,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (dateGroup == "Today" || dateGroup == "Yesterday") Color.Black else Color.DarkGray,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
            }

            items(list) { measurement ->
                MeasurementCard(measurement = measurement)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun MeasurementCard(measurement: Measurement) {
    val color = getConditionColor(measurement.condition)
    val conditionIcon = when (measurement.condition) {
        "Good" -> "💚"
        "Very Good" -> "✅"
        "Warning" -> "🧡"
        "Dangerous" -> "❤️‍🔥"
        else -> "💙"
    }

    // Dynamic SpO2 color
    val spo2Color = when {
        measurement.spo2 >= 95 -> Color(0xFF285702) // bright green
        measurement.spo2 >= 92 -> Color(0xFF41A903) // green
        measurement.spo2 >= 90 -> Color(0xFFFFA000) // orange
        else -> Color(0xFFD50000) // red
    }

    // Cap SpO2 at 100
    val spo2Display = if (measurement.spo2 > 100) 100 else measurement.spo2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // BPM indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${measurement.bpm} bpm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // SpO2 indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(spo2Color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$spo2Display%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = spo2Color
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$conditionIcon ${measurement.condition}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }

            Text(
                text = measurement.formattedTime,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
