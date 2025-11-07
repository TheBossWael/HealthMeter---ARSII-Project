
package com.example.healthmeter.ui.rppg

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.healthmeter.Assets.Screen
import com.example.healthmeter.data.model.Measurement
import com.example.healthmeter.data.repository.addMeasurement
import com.example.healthmeter.ui.rppg.RppgLogic.RppgProcessor
import com.example.healthmeter.ui.rppg.RppgUI.CameraPreviewWithProcessing
import kotlinx.coroutines.delay
import org.opencv.android.OpenCVLoader

private const val TAG = "RppgScreen"

@Composable
fun HeartbeatWebViewScreen(navController: NavHostController? = null) {
    Log.d(TAG, "==================== SCREEN START ====================")
    Log.d(TAG, "Initializing RppgScreen")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    Log.d(TAG, "Camera permission status: $hasCameraPermission")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(TAG, "Camera permission result: $granted")
        hasCameraPermission = granted
    }

    var showResults by remember { mutableStateOf(false) }
    var finalBpm by remember { mutableStateOf(0f) }
    var finalSpo2 by remember { mutableStateOf(0f) }
    var currentBpm by remember { mutableStateOf(0f) }
    var currentSpo2 by remember { mutableStateOf(0f) }
    var measurementProgress by remember { mutableStateOf(0f) }

    // Create RppgProcessor
    Log.d(TAG, "Creating RppgProcessor instance...")
    val rppgProcessor = remember {
        RppgProcessor(
            context = context,
            targetFps = 30,
            onMeasurementComplete = { bpm, spo2 ->
                Log.d(TAG, "⚡ CALLBACK: Measurement complete: BPM=$bpm SpO₂=$spo2")
                finalBpm = bpm
                finalSpo2 = spo2
                showResults = true

                // Save to Firebase
                val measurement = Measurement(
                    bpm = bpm.toInt(),
                    spo2 = spo2.toInt()
                )

                addMeasurement(
                    measurement = measurement,
                    onSuccess = {
                        Log.d(TAG, "✓ Measurement saved to Firebase successfully")
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Failed to save measurement to Firebase", e)
                    }
                )
            },
            onVitalsUpdate = { bpm, spo2 ->
                Log.v(TAG, "Vitals update: BPM=$bpm, SpO2=$spo2")
                currentBpm = bpm
                currentSpo2 = spo2
            }
        )
    }
    Log.d(TAG, "RppgProcessor instance created")

    // Initialize OpenCV
    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect: Initializing OpenCV...")
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "❌ OpenCV initialization FAILED")
        } else {
            Log.d(TAG, "✓ OpenCV initialized successfully")
        }
    }

    // Update progress
    LaunchedEffect(currentBpm) {
        while (!showResults && currentBpm > 0) {
            val progress = rppgProcessor.getCurrentProgress()
            measurementProgress = progress.first.toFloat() / progress.second.toFloat()
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        Log.d(TAG, "DisposableEffect: Screen composed")
        onDispose {
            Log.d(TAG, "DisposableEffect: Disposing screen — stopping processor")
            rppgProcessor.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Log.d(TAG, "Rendering UI with camera permission granted")
            if (!showResults) {
                Log.d(TAG, "Showing measurement screen")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Heart Rate Monitor",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )

                    Text(
                        text = "BPM: ${currentBpm.toInt()} | SpO₂: ${currentSpo2.toInt()}%",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )

                    if (currentBpm > 0) {
                        LinearProgressIndicator(
                            progress = measurementProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                        Text(
                            text = "Measuring... ${(measurementProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Text(
                            text = "Position your face in the frame",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    CameraPreviewWithProcessing(
                        rppgProcessor = rppgProcessor,
                        lifecycleOwner = lifecycleOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Button(
                        onClick = {
                            Log.d(TAG, "Test Random Result button clicked")
                            rppgProcessor.testRandomResult()
                            showResults = true
                        },
                        modifier = Modifier.padding(16.dp, bottom = 0.dp)
                    ) {
                        Text("Test Random Result")
                    }

                    BackToHomeButton(navController)
                }
            } else {
                Log.d(TAG, "Showing results screen")
                ResultsScreen(
                    bpm = finalBpm,
                    spo2 = finalSpo2,
                    onRestart = {
                        Log.d(TAG, "Restart button clicked")
                        showResults = false
                        finalBpm = 0f
                        finalSpo2 = 0f
                        currentBpm = 0f
                        currentSpo2 = 0f
                        measurementProgress = 0f
                        rppgProcessor.restart()
                    },
                    navController = navController
                )
            }
        } else {
            Log.d(TAG, "Showing permission request screen")
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required")
                Button(onClick = {
                    Log.d(TAG, "Grant Permission button clicked")
                    launcher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun BackToHomeButton(navController: NavHostController?) {
    Button(
        onClick = {
            navController?.navigate(Screen.HomeScreen.route) {
                popUpTo("home") { inclusive = true }
            }
        },
        modifier = Modifier.padding(6.dp)
    ) {
        Text("Back to Home")
    }
}

@Composable
fun ResultsScreen(
    bpm: Float,
    spo2: Float,
    onRestart: () -> Unit,
    navController: NavHostController? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Final Results", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Heart Rate: ${bpm.toInt()} BPM", fontSize = 24.sp)
            Text("SpO₂: ${"%.1f".format(spo2)}%", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRestart) {
                Text("Restart Measurement")
            }
            BackToHomeButton(navController)
        }
    }
}