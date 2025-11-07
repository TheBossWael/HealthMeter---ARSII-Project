package com.example.healthmeter.ui.rppg.RppgLogic


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val TAG = "FrameAnalyzer"
private const val RESCAN_INTERVAL = 1000L // 1 second
private const val ALPHA = 0.2 // RSVR parameter from paper

/**
 * Handles face detection and skin region extraction using RSVR method
 * Based on: "Real-time realizable mobile imaging photoplethysmography" (Lee et al., 2022)
 */
class FrameAnalyzer(private val context: Context) {

    private var classifier: CascadeClassifier? = null
    private var faceValid = false
    private var face = Rect()
    private var lastScanTime = 0L
    private var frameCount = 0

    fun initialize() {
        Log.d(TAG, "Initializing FrameAnalyzer...")
        try {
            val cascadeFile = loadCascadeFile()
            classifier = CascadeClassifier(cascadeFile.absolutePath)

            if (classifier?.empty() == true) {
                Log.e(TAG, "Failed to load cascade classifier")
            } else {
                Log.d(TAG, "Cascade classifier loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FrameAnalyzer", e)
        }
    }

    /**
     * Process a camera frame and extract averaged RGB values from facial skin
     * Returns Triple(R, G, B) or null if no valid face detected
     */
    fun processFrame(imageProxy: ImageProxy): Triple<Double, Double, Double>? {
        frameCount++

        try {
            // Convert ImageProxy to Mat
            val bitmap = imageProxy.toBitmap() ?: return null
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            bitmap.recycle()

            // Convert to grayscale for face detection
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.equalizeHist(grayMat, grayMat)

            // Face detection (every RESCAN_INTERVAL ms)
            val time = System.currentTimeMillis()
            if (!faceValid || (time - lastScanTime) >= RESCAN_INTERVAL) {
                lastScanTime = time
                detectFace(grayMat)
            }

            // Extract skin region if face is valid
            val result = if (faceValid) {
                extractSkinPixels(mat, grayMat)
            } else {
                null
            }

            mat.release()
            grayMat.release()

            return result

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame #$frameCount", e)
            return null
        }
    }

    /**
     * Detect face using Haar Cascade with multiple strategies
     */
    private fun detectFace(gray: Mat) {
        if (classifier == null || classifier?.empty() == true) {
            Log.w(TAG, "Classifier not ready")
            return
        }

        // Downscale for better detection
        val scaleFactor = 0.5
        val smallGray = Mat()
        Imgproc.resize(gray, smallGray, Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR)

        val faces = MatOfRect()

        try {
            // Strategy 1: Lenient parameters
            classifier?.detectMultiScale(
                smallGray,
                faces,
                1.05,
                2,
                0,
                Size(50.0, 50.0),
                Size()
            )

            var detected = faces.toArray().size

            // Strategy 2: More lenient if first fails
            if (detected == 0) {
                faces.release()
                val faces2 = MatOfRect()
                classifier?.detectMultiScale(
                    smallGray,
                    faces2,
                    1.03,
                    1,
                    0,
                    Size(30.0, 30.0),
                    Size()
                )
                detected = faces2.toArray().size
                if (detected > 0) {
                    faces2.toArray().copyInto(faces.toArray())
                }
                faces2.release()
            }

            // Strategy 3: Full size with lenient params
            if (detected == 0) {
                faces.release()
                val faces3 = MatOfRect()
                classifier?.detectMultiScale(
                    gray,
                    faces3,
                    1.03,
                    1,
                    0,
                    Size(80.0, 80.0),
                    Size()
                )
                detected = faces3.toArray().size
                if (detected > 0) {
                    face = faces3.toArray()[0]
                    faceValid = true
                    Log.v(TAG, "Face detected (strategy 3): $face")
                    faces3.release()
                    smallGray.release()
                    return
                }
                faces3.release()
            }

            val faceArray = faces.toArray()
            if (faceArray.isNotEmpty()) {
                // Scale coordinates back to original size
                val detectedFace = faceArray[0]
                face = Rect(
                    (detectedFace.x / scaleFactor).toInt(),
                    (detectedFace.y / scaleFactor).toInt(),
                    (detectedFace.width / scaleFactor).toInt(),
                    (detectedFace.height / scaleFactor).toInt()
                )
                faceValid = true
                Log.v(TAG, "Face detected: $face")
            } else {
                invalidateFace()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in face detection", e)
        } finally {
            faces.release()
            smallGray.release()
        }
    }

    /**
     * Extract skin pixels using RSVR (Relative Saturation Value Range) method
     * Returns averaged RGB values from skin region
     */
    private fun extractSkinPixels(rgbMat: Mat, grayMat: Mat): Triple<Double, Double, Double>? {
        try {
            // Validate face rect is within bounds
            if (face.x < 0 || face.y < 0 ||
                face.x + face.width > rgbMat.cols() ||
                face.y + face.height > rgbMat.rows()) {
                Log.w(TAG, "Face rect out of bounds")
                invalidateFace()
                return null
            }

            // Convert face region to HSV
            val faceRoi = rgbMat.submat(face)
            val hsvMat = Mat()
            Imgproc.cvtColor(faceRoi, hsvMat, Imgproc.COLOR_RGB2HSV)

            // Extract saturation channel
            val channels = mutableListOf<Mat>()
            Core.split(hsvMat, channels)
            val saturation = channels[1]

            // Create histogram of S values
            val histSize = 256
            val hist = Mat()
            Imgproc.calcHist(
                listOf(saturation),
                MatOfInt(0),
                Mat(),
                hist,
                MatOfInt(histSize),
                MatOfFloat(0f, 256f)
            )

            // Apply median filter to histogram (length 5)
            val smoothedHist = FloatArray(histSize)
            for (i in 0 until histSize) {
                val windowStart = maxOf(0, i - 2)
                val windowEnd = minOf(histSize - 1, i + 2)
                val values = mutableListOf<Float>()
                for (j in windowStart..windowEnd) {
                    values.add(hist.get(j, 0)[0].toFloat())
                }
                smoothedHist[i] = values.sorted()[values.size / 2]
            }

            // Find histmax (most frequent S value)
            var histmax = 0
            var maxVal = 0f
            for (i in smoothedHist.indices) {
                if (smoothedHist[i] > maxVal) {
                    maxVal = smoothedHist[i]
                    histmax = i
                }
            }

            // Calculate threshold range (RSVR with alpha = 0.2)
            val thRange = (ALPHA * histmax).toInt()
            val lowerBound = maxOf(0, histmax - thRange / 2)
            val upperBound = minOf(255, histmax + thRange / 2)

            Log.v(TAG, "RSVR: histmax=$histmax, range=[$lowerBound, $upperBound]")

            // Create mask for skin pixels
            val mask = Mat.zeros(saturation.rows(), saturation.cols(), CvType.CV_8UC1)
            for (i in 0 until saturation.rows()) {
                for (j in 0 until saturation.cols()) {
                    val sVal = saturation.get(i, j)[0].toInt()
                    if (sVal in lowerBound..upperBound) {
                        mask.put(i, j, 255.0)
                    }
                }
            }

            // Calculate mean RGB values using mask
            val mean = Core.mean(faceRoi, mask)

            // Cleanup
            hsvMat.release()
            channels.forEach { it.release() }
            hist.release()
            mask.release()
            faceRoi.release()

            Log.v(TAG, "Extracted RGB: R=${mean.`val`[0]}, G=${mean.`val`[1]}, B=${mean.`val`[2]}")

            return Triple(mean.`val`[0], mean.`val`[1], mean.`val`[2])

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting skin pixels", e)
            return null
        }
    }

    fun invalidateFace() {
        faceValid = false
        Log.v(TAG, "Face invalidated")
    }

    fun isFaceValid(): Boolean = faceValid

    fun getFaceRect(): Rect = face

    private fun loadCascadeFile(): File {
        val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
        val cascadeFile = File(cascadeDir, "haarcascade_frontalface_alt.xml")

        if (!cascadeFile.exists()) {
            context.assets.open("haarcascade_frontalface_alt.xml").use { input ->
                FileOutputStream(cascadeFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Cascade file copied to ${cascadeFile.absolutePath}")
        }

        return cascadeFile
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        return try {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)

            val bytes = out.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
}