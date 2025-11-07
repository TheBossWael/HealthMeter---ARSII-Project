package com.example.healthmeter.ui.rppg.RppgUI

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.healthmeter.ui.rppg.RppgLogic.RppgProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

@Composable
fun CameraPreviewWithProcessing(
    rppgProcessor: RppgProcessor,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "==================== CAMERA SETUP START ====================")
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    Log.d(TAG, "PreviewView created: $previewView")

    LaunchedEffect(previewView) {
        Log.d(TAG, "LaunchedEffect: Setting up camera...")
        withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Step 1: Getting camera provider...")
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                Log.d(TAG, "Step 1: ✓ Camera provider obtained: $cameraProvider")

                Log.d(TAG, "Step 2: Building preview...")
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                Log.d(TAG, "Step 2: ✓ Preview built: $preview")

                Log.d(TAG, "Step 3: Building image analyzer...")
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .apply {
                        Log.d(TAG, "Step 3a: Setting analyzer executor...")
                        setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            Log.v(TAG, "Frame received: ${imageProxy.width}x${imageProxy.height}, format=${imageProxy.format}")
                            try {
                                rppgProcessor.processFrame(imageProxy)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing frame", e)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }
                Log.d(TAG, "Step 3: ✓ Analyzer built and configured")

                Log.d(TAG, "Step 4: Selecting camera...")
                val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                Log.d(TAG, "Camera availability - Front: $hasFrontCamera, Back: $hasBackCamera")

                val cameraSelector = if (hasFrontCamera) {
                    Log.d(TAG, "Step 4: ✓ Using FRONT camera")
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    Log.d(TAG, "Step 4: ✓ Using BACK camera")
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                Log.d(TAG, "Step 5: Unbinding all previous use cases...")
                cameraProvider.unbindAll()
                Log.d(TAG, "Step 5: ✓ All use cases unbound")

                Log.d(TAG, "Step 6: Binding to lifecycle...")
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analyzer
                )
                Log.d(TAG, "Step 6: ✓ Camera bound successfully: $camera")
                Log.d(TAG, "Camera info: ${camera.cameraInfo}")

                Log.d(TAG, "Step 7: Initializing rPPG processor...")
                rppgProcessor.init()
                Log.d(TAG, "Step 7: ✓ rPPG processor initialized")

                Log.d(TAG, "==================== CAMERA SETUP COMPLETE ====================")
            } catch (e: Exception) {
                Log.e(TAG, "❌ CAMERA SETUP ERROR", e)
                Log.e(TAG, "Error details: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}