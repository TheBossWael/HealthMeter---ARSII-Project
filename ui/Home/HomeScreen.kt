package com.example.healthmeter.ui.Home

import GetCurrentUserName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmeter.Assets.BoxBackgroundLayout
import com.example.healthmeter.data.model.Measurement
import com.example.healthmeter.data.repository.getLastMeasurement

@Composable
fun HomeScreen(
    logout: () -> Unit,
    onNavigateToRppgScreen: () -> Unit
) {
    val userName = GetCurrentUserName() ?: "User"
    var lastMeasurement by remember { mutableStateOf<Measurement?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch the last measurement from Firebase
    LaunchedEffect(Unit) {
        getLastMeasurement(
            onSuccess = {
                lastMeasurement = it
                isLoading = false
            },
            onFailure = {
                isLoading = false
            }
        )
    }

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
                        .align(Alignment.CenterStart)
                        .padding(end = 56.dp)
                ) {
                    Text(
                        text = "Welcome\n$userName!",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp,
                            fontSize = 40.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Let's check on your health now",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    )
                }

                IconButton(
                    onClick = logout,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        tint = Color.White,
                        contentDescription = "Logout",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last Measurement",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001F54)
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                } else if (lastMeasurement != null) {
                    val measurement = lastMeasurement!!
                    val timeText = java.text.SimpleDateFormat(
                        "MMM dd, hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(measurement.timestamp))

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0F7FA), RoundedCornerShape(24.dp))
                            .padding(86.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${measurement.bpm}",
                                fontSize = 90.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001F54)
                            )
                            Text(
                                text = "bpm",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001F54)
                            )
                            Text(
                                text = timeText,
                                fontSize = 19.sp,
                                color = Color(0xFF001F54)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No measurements yet",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { onNavigateToRppgScreen() },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(60.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Start Measurement Now", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Arrow",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    )
}
