package com.example.healthmeter.ui.rppg.RppgLogic


import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.*

private const val TAG = "RppgProcessor"
private const val RPPG_INTERVAL = 250L // Process every 250ms
private const val MEASUREMENT_DURATION = 15000L // 15 seconds

/**
 * Main coordinator for rPPG processing
 * Combines FrameAnalyzer and RppgLogic
 */
class RppgProcessor(
    context: Context,
    private val targetFps: Int = 30,
    private val onMeasurementComplete: (Float, Float) -> Unit,
    private val onVitalsUpdate: (Float, Float) -> Unit
) {
    private val frameAnalyzer = FrameAnalyzer(context)
    private val rppgLogic = RppgLogic(targetFps = targetFps, windowSizeSeconds = 8)

    private var streaming = false
    private var measurementStart: Long? = null
    private val bpmValues = mutableListOf<Float>()
    private val spo2Values = mutableListOf<Float>()
    private var measurementDone = false

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var rppgJob: Job? = null

    fun init() {
        Log.d(TAG, "==================== INIT START ====================")
        Log.d(TAG, "Initializing RppgProcessor...")

        try {
            frameAnalyzer.initialize()
            streaming = true
            measurementDone = false
            startRppgTimer()

            Log.d(TAG, "RppgProcessor initialized successfully")
            Log.d(TAG, "==================== INIT COMPLETE ====================")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RppgProcessor", e)
        }
    }

    /**
     * Process each camera frame
     */
    fun processFrame(imageProxy: ImageProxy) {
        if (!streaming) {
            return
        }

        if (measurementDone) {
            return
        }

        try {
            // Extract RGB values from facial skin region
            val rgb = frameAnalyzer.processFrame(imageProxy)

            if (rgb != null) {
                // Add to signal buffer
                rppgLogic.addSignalSample(
                    r = rgb.first,
                    g = rgb.second,
                    b = rgb.third,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Log.v(TAG, "No valid face detected in frame")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
        }
    }

    /**
     * Start periodic rPPG computation
     */
    private fun startRppgTimer() {
        Log.d(TAG, "Starting rPPG timer with interval ${RPPG_INTERVAL}ms")
        rppgJob = scope.launch {
            var tickCount = 0
            while (isActive && streaming && !measurementDone) {
                delay(RPPG_INTERVAL)
                tickCount++
                Log.v(TAG, "rPPG timer tick #$tickCount")
                computeVitals()
            }
            Log.d(TAG, "rPPG timer stopped")
        }
        Log.d(TAG, "rPPG timer started")
    }

    /**
     * Compute heart rate and SpO2
     */
    private fun computeVitals() {
        if (measurementDone) {
            return
        }

        if (!rppgLogic.hasEnoughSamples()) {
            val currentSize = rppgLogic.getCurrentBufferSize()
            Log.v(TAG, "Waiting for more samples... (${currentSize}/240)")
            return
        }

        try {
            Log.d(TAG, "========== COMPUTING VITALS ==========")

            // Compute BPM and SpO2
            val (bpm, spo2) = rppgLogic.computeHeartRate()

            if (bpm > 0) {
                // Start measurement timer on first valid reading
                if (measurementStart == null) {
                    measurementStart = System.currentTimeMillis()
                    Log.d(TAG, "✓ Measurement started at: $measurementStart")
                }

                // Store values
                bpmValues.add(bpm)
                spo2Values.add(spo2)

                Log.d(TAG, "Valid reading: BPM=$bpm, SpO2=$spo2 (${bpmValues.size} readings so far)")

                // Update UI
                onVitalsUpdate(bpm, spo2)

                // Check if measurement duration reached
                val elapsed = System.currentTimeMillis() - (measurementStart ?: 0)
                val remaining = MEASUREMENT_DURATION - elapsed
                Log.d(TAG, "Time elapsed: ${elapsed}ms, remaining: ${remaining}ms")

                if (elapsed >= MEASUREMENT_DURATION) {
                    Log.d(TAG, "⚡⚡⚡ MEASUREMENT DURATION REACHED ⚡⚡⚡")
                    finishMeasurement()
                }
            } else {
                Log.w(TAG, "Invalid reading: BPM=$bpm, SpO2=$spo2")
            }

            Log.d(TAG, "========== VITALS COMPUTATION END ==========")
        } catch (e: Exception) {
            Log.e(TAG, "Error computing vitals", e)
        }
    }

    /**
     * Finish measurement and report final results
     */
    private fun finishMeasurement() {
        Log.d(TAG, "==================== FINISH MEASUREMENT ====================")
        Log.d(TAG, "Total readings collected:")
        Log.d(TAG, "  BPM values: ${bpmValues.size}")
        Log.d(TAG, "  SpO2 values: ${spo2Values.size}")

        measurementDone = true

        val avgBpm = if (bpmValues.isNotEmpty()) {
            bpmValues.average().toFloat()
        } else {
            0f
        }

        val avgSpo2 = if (spo2Values.isNotEmpty()) {
            spo2Values.average().toFloat()
        } else {
            0f
        }

        Log.d(TAG, "Final averaged results:")
        Log.d(TAG, "  Average BPM: $avgBpm")
        Log.d(TAG, "  Average SpO2: $avgSpo2%")
        Log.d(TAG, "Calling onMeasurementComplete callback...")

        onMeasurementComplete(avgBpm, avgSpo2)

        Log.d(TAG, "✓ Callback completed")
        Log.d(TAG, "Stopping processor...")
        stop()
        Log.d(TAG, "==================== MEASUREMENT FINISHED ====================")
    }

    fun stop() {
        Log.d(TAG, "==================== STOP ====================")
        Log.d(TAG, "Stopping RppgProcessor...")
        Log.d(TAG, "Current state: streaming=$streaming, measurementDone=$measurementDone")

        streaming = false
        measurementDone = true

        Log.d(TAG, "Cancelling rPPG job...")
        rppgJob?.cancel()

        Log.d(TAG, "Resetting rPPG logic...")
        rppgLogic.reset()

        Log.d(TAG, "Invalidating face...")
        frameAnalyzer.invalidateFace()

        Log.d(TAG, "RppgProcessor stopped")
        Log.d(TAG, "==================== STOP COMPLETE ====================")
    }

    fun restart() {
        Log.d(TAG, "==================== RESTART ====================")
        Log.d(TAG, "Restarting RppgProcessor...")

        stop()

        Log.d(TAG, "Resetting measurement state...")
        measurementStart = null
        measurementDone = false

        Log.d(TAG, "Clearing stored values...")
        Log.d(TAG, "  Previous BPM values: ${bpmValues.size}")
        Log.d(TAG, "  Previous SpO2 values: ${spo2Values.size}")
        bpmValues.clear()
        spo2Values.clear()
        Log.d(TAG, "  ✓ Values cleared")

        Log.d(TAG, "Reinitializing...")
        init()

        Log.d(TAG, "==================== RESTART COMPLETE ====================")
    }

    /**
     * Test function to generate random results
     */
    fun testRandomResult() {
        Log.d(TAG, "==================== TEST RANDOM RESULT ====================")
        Log.d(TAG, "Stopping current measurement...")
        stop()

        val randomBpm = (60..100).random().toFloat()
        val randomSpo2 = (95.0 + Math.random() * 4.0).toFloat()

        Log.d(TAG, "Generated random values:")
        Log.d(TAG, "  BPM: $randomBpm")
        Log.d(TAG, "  SpO2: $randomSpo2")
        Log.d(TAG, "Calling onMeasurementComplete callback...")

        onMeasurementComplete(randomBpm, randomSpo2)

        Log.d(TAG, "✓ Callback completed")
        Log.d(TAG, "==================== TEST COMPLETE ====================")
    }

    fun isFaceDetected(): Boolean = frameAnalyzer.isFaceValid()

    fun getCurrentProgress(): Pair<Int, Int> {
        val elapsed = if (measurementStart != null) {
            (System.currentTimeMillis() - measurementStart!!).toInt()
        } else {
            0
        }
        return Pair(elapsed, MEASUREMENT_DURATION.toInt())
    }
}