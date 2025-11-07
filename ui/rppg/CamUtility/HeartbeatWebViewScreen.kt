package com.example.healthmeter

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.healthmeter.Assets.Screen
import com.example.healthmeter.data.model.Measurement
import com.example.healthmeter.data.repository.addMeasurement
import kotlinx.coroutines.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.*

private const val TAG_SCREEN = "HeartbeatScreen"
private const val TAG_HEARTBEAT = "Heartbeat"
private const val TAG_CAMERA = "CameraPreview"
private const val RESCAN_INTERVAL = 1000L
private const val DEFAULT_FPS = 30
private const val LOW_BPM = 60
private const val HIGH_BPM = 150
private const val SEC_PER_MIN = 60
private const val MEASUREMENT_DURATION = 15000L

@Composable
fun HeartbeatWebViewScreen(navController: NavHostController? = null) {
    Log.d(TAG_SCREEN, "==================== SCREEN START ====================")
    Log.d(TAG_SCREEN, "Initializing HeartbeatWebViewScreen")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    Log.d(TAG_SCREEN, "Camera permission status: $hasCameraPermission")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d(TAG_SCREEN, "Camera permission result: $granted")
        hasCameraPermission = granted
    }

    var showResults by remember { mutableStateOf(false) }
    var finalBpm by remember { mutableStateOf(0f) }
    var finalSpo2 by remember { mutableStateOf(0f) }
    var currentBpm by remember { mutableStateOf(0f) }
    var currentSpo2 by remember { mutableStateOf(0f) }

    Log.d(TAG_SCREEN, "Creating Heartbeat instance...")
    val heartbeat = remember {
        Heartbeat(
            context = context,
            targetFps = 30,
            windowSize = 6,
            rppgInterval = 250L,
            onMeasurementComplete = { bpm, spo2 ->
                Log.d(TAG_HEARTBEAT, "⚡ CALLBACK: Measurement complete: BPM=$bpm SpO₂=$spo2")
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
                        Log.d(TAG_HEARTBEAT, "Measurement saved to Firebase successfully")
                    },
                    onFailure = { e ->
                        Log.e(TAG_HEARTBEAT, "Failed to save measurement to Firebase", e)
                    }
                )
            },
            onVitalsUpdate = { bpm, spo2 ->
                currentBpm = bpm
                currentSpo2 = spo2
            }
        )
    }

    Log.d(TAG_SCREEN, "Heartbeat instance created: $heartbeat")

    LaunchedEffect(Unit) {
        Log.d(TAG_SCREEN, "LaunchedEffect: Initializing OpenCV...")
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG_SCREEN, "❌ OpenCV initialization FAILED")
        } else {
            Log.d(TAG_SCREEN, "✓ OpenCV initialized successfully")
        }
    }

    DisposableEffect(Unit) {
        Log.d(TAG_SCREEN, "DisposableEffect: Screen composed")
        onDispose {
            Log.d(TAG_SCREEN, "DisposableEffect: Disposing screen — stopping heartbeat")
            heartbeat.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Log.d(TAG_SCREEN, "Rendering UI with camera permission granted")
            if (!showResults) {
                Log.d(TAG_SCREEN, "Showing measurement screen")
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Heartbeat Demo", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
                    Text(
                        text = "BPM: ${currentBpm.toInt()} | SpO₂: ${currentSpo2.toInt()}%",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )

                    CameraPreviewWithProcessing(
                        heartbeat = heartbeat,
                        lifecycleOwner = lifecycleOwner,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )

                    Button(
                        onClick = {
                            Log.d(TAG_SCREEN, "Test Random Result button clicked")
                            heartbeat.testRandomResult()
                            showResults = true
                        },
                        modifier = Modifier.padding(16.dp, bottom = 0.dp)
                    ) { Text("Test Random Result") }
                    BackToHomeButton(navController)

                }
            } else {
                Log.d(TAG_SCREEN, "Showing results screen")
                ResultsScreen(
                    bpm = finalBpm,
                    spo2 = finalSpo2,
                    onRestart = {
                        Log.d(TAG_SCREEN, "Restart button clicked")
                        showResults = false
                        finalBpm = 0f
                        finalSpo2 = 0f
                        heartbeat.restart()
                    }
                )
            }
        } else {
            Log.d(TAG_SCREEN, "Showing permission request screen")
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required")
                Button(onClick = {
                    Log.d(TAG_SCREEN, "Grant Permission button clicked")
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
                popUpTo("home") { inclusive = true } // clears back stack
            }
        },
        modifier = Modifier.padding(6.dp)
    ) {
        Text("Back to Home")
    }
}

@Composable
fun CameraPreviewWithProcessing(
    heartbeat: Heartbeat,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    Log.d(TAG_CAMERA, "==================== CAMERA SETUP START ====================")
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    Log.d(TAG_CAMERA, "PreviewView created: $previewView")

    LaunchedEffect(previewView) {
        Log.d(TAG_CAMERA, "LaunchedEffect: Setting up camera...")
        withContext(Dispatchers.Main) {
            try {
                Log.d(TAG_CAMERA, "Step 1: Getting camera provider...")
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                Log.d(TAG_CAMERA, "Step 1: ✓ Camera provider obtained: $cameraProvider")

                Log.d(TAG_CAMERA, "Step 2: Building preview...")
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                Log.d(TAG_CAMERA, "Step 2: ✓ Preview built: $preview")

                Log.d(TAG_CAMERA, "Step 3: Building image analyzer...")
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .apply {
                        Log.d(TAG_CAMERA, "Step 3a: Setting analyzer executor...")
                        setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            Log.v(TAG_CAMERA, "Frame received: ${imageProxy.width}x${imageProxy.height}, format=${imageProxy.format}, timestamp=${imageProxy.imageInfo.timestamp}")
                            try {
                                heartbeat.processFrame(imageProxy)
                            } catch (e: Exception) {
                                Log.e(TAG_CAMERA, "Error in frame processing", e)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }
                Log.d(TAG_CAMERA, "Step 3: ✓ Analyzer built and configured")

                Log.d(TAG_CAMERA, "Step 4: Selecting camera...")
                val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                Log.d(TAG_CAMERA, "Camera availability - Front: $hasFrontCamera, Back: $hasBackCamera")

                val cameraSelector = if (hasFrontCamera) {
                    Log.d(TAG_CAMERA, "Step 4: ✓ Using FRONT camera")
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    Log.d(TAG_CAMERA, "Step 4: ✓ Using BACK camera")
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                Log.d(TAG_CAMERA, "Step 5: Unbinding all previous use cases...")
                cameraProvider.unbindAll()
                Log.d(TAG_CAMERA, "Step 5: ✓ All use cases unbound")

                Log.d(TAG_CAMERA, "Step 6: Binding to lifecycle...")
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analyzer
                )
                Log.d(TAG_CAMERA, "Step 6: ✓ Camera bound successfully: $camera")
                Log.d(TAG_CAMERA, "Camera info: ${camera.cameraInfo}")

                Log.d(TAG_CAMERA, "Step 7: Initializing heartbeat processing...")
                heartbeat.init()
                Log.d(TAG_CAMERA, "Step 7: ✓ Heartbeat initialized")

                Log.d(TAG_CAMERA, "==================== CAMERA SETUP COMPLETE ====================")
            } catch (e: Exception) {
                Log.e(TAG_CAMERA, "❌ CAMERA SETUP ERROR", e)
                Log.e(TAG_CAMERA, "Error details: ${e.message}")
                Log.e(TAG_CAMERA, "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
fun ResultsScreen(bpm: Float, spo2: Float, onRestart: () -> Unit, navController: NavHostController? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Final Results", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Heart Rate: ${bpm.toInt()} BPM", fontSize = 24.sp)
            Text("SpO₂: ${"%.1f".format(spo2)}%", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRestart) { Text("Restart Measurement") }
            BackToHomeButton(navController)
        }
    }
}


// ------------------------- HEARTBEAT CLASS -------------------------
class Heartbeat(
    private val context: Context,
    private val targetFps: Int,
    private val windowSize: Int,
    private val rppgInterval: Long,
    private val onMeasurementComplete: (Float, Float) -> Unit,
    private val onVitalsUpdate: (Float, Float) -> Unit
) {
    private var streaming = false
    private var faceValid = false
    private var face = Rect()
    private val signal = mutableListOf<DoubleArray>()
    private val timestamps = mutableListOf<Long>()
    private val rescan = mutableListOf<Boolean>()
    private var lastScanTime = 0L

    private var classifier: CascadeClassifier? = null
    private var measurementStart: Long? = null
    private val bpmValues = mutableListOf<Float>()
    private val spo2Values = mutableListOf<Float>()
    private var measurementDone = false
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var rppgJob: Job? = null

    private var frameCount = 0

    fun init() {
        Log.i(TAG_HEARTBEAT, "==================== INIT START ====================")
        Log.i(TAG_HEARTBEAT, "Target FPS: $targetFps, Window size: $windowSize, RPPG interval: ${rppgInterval}ms")
        Log.i(TAG_HEARTBEAT, "Required signal size: ${targetFps * windowSize}")

        try {
            Log.d(TAG_HEARTBEAT, "Step 1: Loading cascade file...")
            val cascadeFile = loadCascadeFile()
            Log.d(TAG_HEARTBEAT, "Step 1: ✓ Cascade file loaded")

            Log.d(TAG_HEARTBEAT, "Step 2: Creating classifier from file...")
            classifier = CascadeClassifier(cascadeFile.absolutePath)

            if (classifier == null) {
                Log.e(TAG_HEARTBEAT, "Step 2: ❌ Classifier is NULL")
                return
            } else if (classifier?.empty() == true) {
                Log.e(TAG_HEARTBEAT, "Step 2: ❌ Classifier is EMPTY")
                return
            } else {
                Log.d(TAG_HEARTBEAT, "Step 2: ✓ Cascade classifier loaded successfully")
            }

            Log.d(TAG_HEARTBEAT, "Step 3: Setting streaming flag to true...")
            streaming = true
            Log.d(TAG_HEARTBEAT, "Step 3: ✓ Streaming enabled")

            Log.d(TAG_HEARTBEAT, "Step 4: Starting RPPG timer...")
            startRppgTimer()
            Log.d(TAG_HEARTBEAT, "Step 4: ✓ RPPG timer started")

            Log.i(TAG_HEARTBEAT, "==================== INIT COMPLETE ====================")
        } catch (e: Exception) {
            Log.e(TAG_HEARTBEAT, "❌ ERROR initializing Heartbeat", e)
            Log.e(TAG_HEARTBEAT, "Error message: ${e.message}")
            Log.e(TAG_HEARTBEAT, "Stack trace: ${e.stackTraceToString()}")
        }
    }

    private fun loadCascadeFile(): File {
        Log.d(TAG_HEARTBEAT, "loadCascadeFile: Start")
        val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
        Log.d(TAG_HEARTBEAT, "loadCascadeFile: Cascade dir: ${cascadeDir.absolutePath}")

        val cascadeFile = File(cascadeDir, "haarcascade_frontalface_alt.xml")
        Log.d(TAG_HEARTBEAT, "loadCascadeFile: Target file: ${cascadeFile.absolutePath}")

        if (!cascadeFile.exists()) {
            Log.d(TAG_HEARTBEAT, "loadCascadeFile: File doesn't exist, copying from assets...")
            try {
                context.assets.open("haarcascade_frontalface_alt.xml").use { input ->
                    FileOutputStream(cascadeFile).use { output ->
                        val bytes = input.copyTo(output)
                        Log.d(TAG_HEARTBEAT, "loadCascadeFile: Copied $bytes bytes")
                    }
                }
                Log.d(TAG_HEARTBEAT, "loadCascadeFile: ✓ File copied successfully")
            } catch (e: Exception) {
                Log.e(TAG_HEARTBEAT, "loadCascadeFile: ❌ Error copying file", e)
                throw e
            }
        } else {
            Log.d(TAG_HEARTBEAT, "loadCascadeFile: File already exists")
        }

        Log.d(TAG_HEARTBEAT, "loadCascadeFile: File exists: ${cascadeFile.exists()}, Size: ${cascadeFile.length()} bytes, Readable: ${cascadeFile.canRead()}")
        return cascadeFile
    }

    fun processFrame(imageProxy: ImageProxy) {
        frameCount++

        if (!streaming) {
            Log.w(TAG_HEARTBEAT, "Frame #$frameCount: Streaming is FALSE, skipping")
            return
        }

        Log.d(TAG_HEARTBEAT, "========== FRAME #$frameCount START ==========")
        Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Size=${imageProxy.width}x${imageProxy.height}, Format=${imageProxy.format}")

        try {
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 1 - Converting to bitmap...")
            val bitmap = imageProxy.toBitmap()
            if (bitmap == null) {
                Log.e(TAG_HEARTBEAT, "Frame #$frameCount: Step 1 - ❌ Bitmap is NULL")
                return
            }
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 1 - ✓ Bitmap created: ${bitmap.width}x${bitmap.height}")

            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 2 - Converting bitmap to Mat...")
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 2 - ✓ Mat created: ${mat.cols()}x${mat.rows()}, channels=${mat.channels()}, type=${mat.type()}")
            bitmap.recycle()

            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 3 - Converting to grayscale...")
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 3 - ✓ Gray mat created: ${grayMat.cols()}x${grayMat.rows()}, type=${grayMat.type()}")

            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 4 - Applying histogram equalization...")
            Imgproc.equalizeHist(grayMat, grayMat)
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 4 - ✓ Histogram equalized")

            val time = System.currentTimeMillis()
            val timeSinceLastScan = time - lastScanTime
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 5 - Face detection check")
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: faceValid=$faceValid, timeSinceLastScan=${timeSinceLastScan}ms")

            if (!faceValid || timeSinceLastScan >= RESCAN_INTERVAL) {
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 5 - Triggering face detection...")
                lastScanTime = time
                detectFace(grayMat)
            } else {
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 5 - Skipping face detection (using existing face)")
            }

            if (faceValid) {
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6 - Face is VALID, extracting signal...")
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Face rect: x=${face.x}, y=${face.y}, w=${face.width}, h=${face.height}")

                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6a - Creating mask...")
                val mask = makeMask(grayMat, face)
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6a - ✓ Mask created: ${mask.cols()}x${mask.rows()}")

                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6b - Calculating mean color...")
                val mean = Core.mean(mat, mask)
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6b - ✓ Mean calculated: R=${mean.`val`[0]}, G=${mean.`val`[1]}, B=${mean.`val`[2]}")
                mask.release()

                signal.add(doubleArrayOf(mean.`val`[0], mean.`val`[1], mean.`val`[2]))
                timestamps.add(time)
                rescan.add(false)
                Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Step 6c - ✓ Signal added. Total signal size: ${signal.size}")
            } else {
                Log.w(TAG_HEARTBEAT, "Frame #$frameCount: Step 6 - Face is INVALID, skipping signal extraction")
            }

            mat.release()
            grayMat.release()
            Log.d(TAG_HEARTBEAT, "Frame #$frameCount: Mats released")
            Log.d(TAG_HEARTBEAT, "========== FRAME #$frameCount END ==========")
        } catch (e: Exception) {
            Log.e(TAG_HEARTBEAT, "Frame #$frameCount: ❌ ERROR processing frame", e)
            Log.e(TAG_HEARTBEAT, "Frame #$frameCount: Error message: ${e.message}")
            Log.e(TAG_HEARTBEAT, "Frame #$frameCount: Stack trace: ${e.stackTraceToString()}")
        }
    }

    private fun detectFace(gray: Mat) {
        Log.d(TAG_HEARTBEAT, "detectFace: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "detectFace: Input gray mat: ${gray.cols()}x${gray.rows()}, type=${gray.type()}, channels=${gray.channels()}")

        if (classifier == null) {
            Log.e(TAG_HEARTBEAT, "detectFace: ❌ Classifier is NULL")
            return
        }

        if (classifier?.empty() == true) {
            Log.e(TAG_HEARTBEAT, "detectFace: ❌ Classifier is EMPTY")
            return
        }

        Log.d(TAG_HEARTBEAT, "detectFace: Classifier is valid, proceeding...")

        // Downscale for better face detection
        val scaleFactor = 0.5 // Scale to 50%
        val smallGray = Mat()
        Imgproc.resize(gray, smallGray, Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR)
        Log.d(TAG_HEARTBEAT, "detectFace: Downscaled to: ${smallGray.cols()}x${smallGray.rows()}")

        val faces = MatOfRect()
        var detected = 0

        try {
            // Strategy 1: Lenient parameters
            Log.d(TAG_HEARTBEAT, "detectFace: Strategy 1 - Lenient detection...")
            classifier?.detectMultiScale(
                smallGray,
                faces,
                1.05,
                2,
                0,
                Size(50.0, 50.0),
                Size()
            )
            detected = faces.toArray().size
            Log.d(TAG_HEARTBEAT, "detectFace: Strategy 1 returned $detected faces")

            // Strategy 2: Very lenient if first fails
            if (detected == 0) {
                Log.d(TAG_HEARTBEAT, "detectFace: Strategy 2 - Very lenient detection...")
                faces.release()
                val faces2 = MatOfRect()
                classifier?.detectMultiScale(
                    smallGray,
                    faces2,
                    1.03,              // Even more thorough
                    1,                 // Minimal neighbors
                    0,
                    Size(30.0, 30.0), // Even smaller min size
                    Size()
                )
                detected = faces2.toArray().size
                Log.d(TAG_HEARTBEAT, "detectFace: Strategy 2 returned $detected faces")

                if (detected > 0) {
                    faces2.toArray().copyInto(faces.toArray())
                }
                faces2.release()
            }

            // Strategy 3: Try on full-size image with very lenient params
            if (detected == 0) {
                Log.d(TAG_HEARTBEAT, "detectFace: Strategy 3 - Full size with very lenient params...")
                faces.release()
                val faces3 = MatOfRect()
                classifier?.detectMultiScale(
                    gray,  // Use original size
                    faces3,
                    1.03,
                    1,
                    0,
                    Size(80.0, 80.0),
                    Size()
                )
                detected = faces3.toArray().size
                Log.d(TAG_HEARTBEAT, "detectFace: Strategy 3 returned $detected faces")

                if (detected > 0) {
                    // No need to scale back
                    face = faces3.toArray()[0]
                    faceValid = true
                    Log.d(TAG_HEARTBEAT, "detectFace: ✓✓✓ FACE VALIDATED (Strategy 3) ✓✓✓")
                    Log.d(TAG_HEARTBEAT, "detectFace: Selected face: x=${face.x}, y=${face.y}, w=${face.width}, h=${face.height}")
                    faces3.release()
                    return
                }
                faces3.release()
            }

            val faceArray = faces.toArray()
            detected = faceArray.size

            if (detected > 0) {
                Log.d(TAG_HEARTBEAT, "detectFace: Processing ${detected} detected faces:")
                for (i in faceArray.indices) {
                    Log.d(TAG_HEARTBEAT, "detectFace:   Face[$i]: x=${faceArray[i].x}, y=${faceArray[i].y}, w=${faceArray[i].width}, h=${faceArray[i].height}")
                }

                // Scale coordinates back to original size
                val detectedFace = faceArray[0]
                face = Rect(
                    (detectedFace.x / scaleFactor).toInt(),
                    (detectedFace.y / scaleFactor).toInt(),
                    (detectedFace.width / scaleFactor).toInt(),
                    (detectedFace.height / scaleFactor).toInt()
                )
                faceValid = true
                Log.d(TAG_HEARTBEAT, "detectFace: ✓✓✓ FACE VALIDATED ✓✓✓")
                Log.d(TAG_HEARTBEAT, "detectFace: Selected face (scaled back): x=${face.x}, y=${face.y}, w=${face.width}, h=${face.height}")
            } else {
                Log.w(TAG_HEARTBEAT, "detectFace: ❌ NO FACES DETECTED (all strategies failed)")
                if (faceValid) {
                    Log.w(TAG_HEARTBEAT, "detectFace: Previously had valid face, now lost - invalidating")
                }
                invalidateFace()
            }
        } catch (e: Exception) {
            Log.e(TAG_HEARTBEAT, "detectFace: ❌ ERROR in detectMultiScale", e)
            Log.e(TAG_HEARTBEAT, "detectFace: Error message: ${e.message}")
        } finally {
            faces.release()
            smallGray.release()
            Log.d(TAG_HEARTBEAT, "detectFace: Faces MatOfRect and smallGray released")
        }

        Log.d(TAG_HEARTBEAT, "detectFace: ========== END (faceValid=$faceValid) ==========")
    }

    private fun makeMask(frameGray: Mat, face: Rect): Mat {
        Log.d(TAG_HEARTBEAT, "makeMask: Creating mask for face region")
        Log.d(TAG_HEARTBEAT, "makeMask: Frame size: ${frameGray.cols()}x${frameGray.rows()}")
        Log.d(TAG_HEARTBEAT, "makeMask: Face rect: x=${face.x}, y=${face.y}, w=${face.width}, h=${face.height}")

        val result = Mat.zeros(frameGray.rows(), frameGray.cols(), CvType.CV_8UC1)

        val startX = (face.x + 0.3 * face.width).toInt()
        val startY = (face.y + 0.1 * face.height).toInt()
        val endX = (face.x + 0.7 * face.width).toInt()
        val endY = (face.y + 0.25 * face.height).toInt()

        Log.d(TAG_HEARTBEAT, "makeMask: ROI rectangle: ($startX,$startY) to ($endX,$endY)")

        Imgproc.rectangle(
            result,
            Point(startX.toDouble(), startY.toDouble()),
            Point(endX.toDouble(), endY.toDouble()),
            Scalar(255.0, 255.0, 255.0),
            -1
        )

        Log.d(TAG_HEARTBEAT, "makeMask: ✓ Mask created successfully")
        return result
    }

    private fun invalidateFace() {
        Log.d(TAG_HEARTBEAT, "invalidateFace: START")
        val previousSignalSize = signal.size

        signal.clear()
        timestamps.clear()
        rescan.clear()
        faceValid = false

        Log.d(TAG_HEARTBEAT, "invalidateFace: Cleared signal (was $previousSignalSize samples)")
        Log.d(TAG_HEARTBEAT, "invalidateFace: faceValid set to FALSE")
        Log.d(TAG_HEARTBEAT, "invalidateFace: END")
    }

    private fun startRppgTimer() {
        Log.d(TAG_HEARTBEAT, "startRppgTimer: Creating coroutine job...")
        rppgJob = scope.launch {
            Log.d(TAG_HEARTBEAT, "startRppgTimer: Coroutine started, interval=${rppgInterval}ms")
            var tickCount = 0
            while (isActive) {
                delay(rppgInterval)
                tickCount++
                Log.d(TAG_HEARTBEAT, "rppgTimer: Tick #$tickCount")
                rppg()
            }
            Log.d(TAG_HEARTBEAT, "startRppgTimer: Coroutine ended")
        }
        Log.d(TAG_HEARTBEAT, "startRppgTimer: ✓ Timer started, job=$rppgJob")
    }

    private fun rppg() {
        val currentSignalSize = signal.size
        val requiredSize = targetFps * windowSize

        Log.d(TAG_HEARTBEAT, "rppg: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "rppg: Signal size: $currentSignalSize / $requiredSize required")
        Log.d(TAG_HEARTBEAT, "rppg: Face valid: $faceValid")
        Log.d(TAG_HEARTBEAT, "rppg: Measurement done: $measurementDone")

        if (currentSignalSize < requiredSize) {
            Log.w(TAG_HEARTBEAT, "rppg: ⚠ Not enough signal data yet (need ${requiredSize - currentSignalSize} more samples)")
            Log.d(TAG_HEARTBEAT, "rppg: ========== END (insufficient data) ==========")
            return
        }

        try {
            Log.d(TAG_HEARTBEAT, "rppg: Processing signal...")

            // Simulated values for debugging
            val bpm = (60..100).random().toFloat()
            val spo2 = (95..99).random().toFloat()
            Log.d(TAG_HEARTBEAT, "rppg: Generated values - BPM: $bpm, SpO2: $spo2")

            if (measurementStart == null) {
                measurementStart = System.currentTimeMillis()
                Log.d(TAG_HEARTBEAT, "rppg: ✓ Measurement started at: $measurementStart")
            }

            bpmValues.add(bpm)
            spo2Values.add(spo2)
            Log.d(TAG_HEARTBEAT, "rppg: Values stored. Total readings: BPM=${bpmValues.size}, SpO2=${spo2Values.size}")

            Log.d(TAG_HEARTBEAT, "rppg: Calling onVitalsUpdate callback...")
            onVitalsUpdate(bpm, spo2)
            Log.d(TAG_HEARTBEAT, "rppg: ✓ Callback completed")

            val elapsed = System.currentTimeMillis() - (measurementStart ?: 0)
            val remaining = MEASUREMENT_DURATION - elapsed
            Log.d(TAG_HEARTBEAT, "rppg: Time - Elapsed: ${elapsed}ms, Remaining: ${remaining}ms")

            if (elapsed >= MEASUREMENT_DURATION) {
                Log.d(TAG_HEARTBEAT, "rppg: ⚡⚡⚡ MEASUREMENT DURATION REACHED ⚡⚡⚡")
                finishMeasurement()
            }
        } catch (e: Exception) {
            Log.e(TAG_HEARTBEAT, "rppg: ❌ ERROR", e)
            Log.e(TAG_HEARTBEAT, "rppg: Error message: ${e.message}")
            Log.e(TAG_HEARTBEAT, "rppg: Stack trace: ${e.stackTraceToString()}")
        }

        Log.d(TAG_HEARTBEAT, "rppg: ========== END ==========")
    }

    private fun finishMeasurement() {
        Log.d(TAG_HEARTBEAT, "finishMeasurement: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "finishMeasurement: Total BPM readings: ${bpmValues.size}")
        Log.d(TAG_HEARTBEAT, "finishMeasurement: Total SpO2 readings: ${spo2Values.size}")

        measurementDone = true

        val avgBpm = if (bpmValues.isNotEmpty()) bpmValues.average().toFloat() else 0f
        val avgSpo2 = if (spo2Values.isNotEmpty()) spo2Values.average().toFloat() else 0f

        Log.d(TAG_HEARTBEAT, "finishMeasurement: Average BPM: $avgBpm")
        Log.d(TAG_HEARTBEAT, "finishMeasurement: Average SpO2: $avgSpo2%")
        Log.d(TAG_HEARTBEAT, "finishMeasurement: Calling onMeasurementComplete callback...")

        onMeasurementComplete(avgBpm, avgSpo2)

        Log.d(TAG_HEARTBEAT, "finishMeasurement: ✓ Callback completed")
        Log.d(TAG_HEARTBEAT, "finishMeasurement: Calling stop()...")
        stop()
        Log.d(TAG_HEARTBEAT, "finishMeasurement: ========== END ==========")
    }

    fun stop() {
        Log.d(TAG_HEARTBEAT, "stop: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "stop: Current state - streaming=$streaming, measurementDone=$measurementDone")

        streaming = false
        measurementDone = true

        Log.d(TAG_HEARTBEAT, "stop: Cancelling RPPG job...")
        rppgJob?.cancel()
        Log.d(TAG_HEARTBEAT, "stop: ✓ RPPG job cancelled")

        Log.d(TAG_HEARTBEAT, "stop: Invalidating face...")
        invalidateFace()

        Log.d(TAG_HEARTBEAT, "stop: Final state - streaming=$streaming, signal.size=${signal.size}")
        Log.d(TAG_HEARTBEAT, "stop: ========== END ==========")
    }

    fun restart() {
        Log.d(TAG_HEARTBEAT, "restart: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "restart: Resetting measurement state...")

        measurementDone = false
        measurementStart = null

        Log.d(TAG_HEARTBEAT, "restart: Clearing stored values...")
        Log.d(TAG_HEARTBEAT, "restart: Previous BPM values: ${bpmValues.size}")
        Log.d(TAG_HEARTBEAT, "restart: Previous SpO2 values: ${spo2Values.size}")
        bpmValues.clear()
        spo2Values.clear()
        Log.d(TAG_HEARTBEAT, "restart: ✓ Values cleared")

        Log.d(TAG_HEARTBEAT, "restart: Invalidating face...")
        invalidateFace()

        Log.d(TAG_HEARTBEAT, "restart: Reinitializing...")
        frameCount = 0
        init()

        Log.d(TAG_HEARTBEAT, "restart: ========== END ==========")
    }

    fun testRandomResult() {
        Log.d(TAG_HEARTBEAT, "testRandomResult: ========== START ==========")
        Log.d(TAG_HEARTBEAT, "testRandomResult: Stopping current measurement...")
        stop()

        val randomBpm = (60..150).random().toFloat()
        val randomSpo2 = (93.0 + Math.random() * (99.0 - 93.0)).toFloat()

        Log.d(TAG_HEARTBEAT, "testRandomResult: Generated random values:")
        Log.d(TAG_HEARTBEAT, "testRandomResult:   BPM: $randomBpm")
        Log.d(TAG_HEARTBEAT, "testRandomResult:   SpO2: $randomSpo2")
        Log.d(TAG_HEARTBEAT, "testRandomResult: Calling onMeasurementComplete callback...")

        onMeasurementComplete(randomBpm, randomSpo2)

        Log.d(TAG_HEARTBEAT, "testRandomResult: ✓ Callback completed")
        Log.d(TAG_HEARTBEAT, "testRandomResult: ========== END ==========")
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        Log.v(TAG_HEARTBEAT, "toBitmap: Converting ImageProxy to Bitmap...")
        return try {
            Log.v(TAG_HEARTBEAT, "toBitmap: ImageProxy planes count: ${planes.size}")

            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            Log.v(TAG_HEARTBEAT, "toBitmap: Buffer sizes - Y:$ySize, U:$uSize, V:$vSize")

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            Log.v(TAG_HEARTBEAT, "toBitmap: Creating YuvImage...")
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)

            val out = ByteArrayOutputStream()
            Log.v(TAG_HEARTBEAT, "toBitmap: Compressing to JPEG...")
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)

            val bytes = out.toByteArray()
            Log.v(TAG_HEARTBEAT, "toBitmap: JPEG size: ${bytes.size} bytes")

            Log.v(TAG_HEARTBEAT, "toBitmap: Decoding to Bitmap...")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (bitmap != null) {
                Log.v(TAG_HEARTBEAT, "toBitmap: ✓ Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.e(TAG_HEARTBEAT, "toBitmap: ❌ Bitmap is NULL after decoding")
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG_HEARTBEAT, "toBitmap: ❌ ERROR converting to Bitmap", e)
            Log.e(TAG_HEARTBEAT, "toBitmap: Error message: ${e.message}")
            null
        }
    }
}