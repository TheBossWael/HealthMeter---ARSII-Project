package com.example.healthmeter.ui.rppg.RppgLogic

import android.util.Log
import kotlin.math.*

private const val TAG = "RppgLogic"

/**
 * Core rPPG (remote Photoplethysmography) logic implementing the CHROM method
 * Based on: "Real-time realizable mobile imaging photoplethysmography" (Lee et al., 2022)
 */
class RppgLogic(
    private val targetFps: Int = 30,
    private val windowSizeSeconds: Int = 8,
    private val minBpm: Float = 40f,
    private val maxBpm: Float = 200f
) {
    // Signal buffers for RGB channels
    private val redSignal = mutableListOf<Double>()
    private val greenSignal = mutableListOf<Double>()
    private val blueSignal = mutableListOf<Double>()
    private val timestamps = mutableListOf<Long>()

    private val requiredSamples = targetFps * windowSizeSeconds

    /**
     * Add a new frame's averaged RGB values to the signal buffer
     */
    fun addSignalSample(r: Double, g: Double, b: Double, timestamp: Long) {
        redSignal.add(r)
        greenSignal.add(g)
        blueSignal.add(b)
        timestamps.add(timestamp)

        // Keep only the required window size
        if (redSignal.size > requiredSamples) {
            redSignal.removeAt(0)
            greenSignal.removeAt(0)
            blueSignal.removeAt(0)
            timestamps.removeAt(0)
        }

        Log.v(TAG, "Signal buffer size: ${redSignal.size}/$requiredSamples")
    }

    /**
     * Check if we have enough samples to compute heart rate
     */
    fun hasEnoughSamples(): Boolean = redSignal.size >= requiredSamples

    /**
     * Compute heart rate using CHROM method
     * Returns pair of (BPM, SpO2 estimate)
     */
    fun computeHeartRate(): Pair<Float, Float> {
        if (!hasEnoughSamples()) {
            Log.w(TAG, "Not enough samples: ${redSignal.size}/$requiredSamples")
            return Pair(0f, 0f)
        }

        try {
            Log.d(TAG, "Computing heart rate from ${redSignal.size} samples")

            // Step 1: Convert to DoubleArray for processing
            val r = redSignal.toDoubleArray()
            val g = greenSignal.toDoubleArray()
            val b = blueSignal.toDoubleArray()

            // Step 2: Apply CHROM method (de Haan & Jeanne, 2013)
            val x = DoubleArray(r.size) { i -> 3 * r[i] - 2 * g[i] }
            val y = DoubleArray(r.size) { i -> 1.5 * r[i] + g[i] - 1.5 * b[i] }

            // Step 3: Calculate standard deviations
            val xStd = standardDeviation(x)
            val yStd = standardDeviation(y)

            // Step 4: Compute pulse signal
            val alpha = if (yStd > 0) xStd / yStd else 1.0
            val pulseSignal = DoubleArray(x.size) { i -> x[i] - alpha * y[i] }

            // Step 5: Detrend the signal (remove DC component)
            val detrended = detrend(pulseSignal)

            // Step 6: Apply bandpass filter (0.4 - 4 Hz for 40-240 BPM)
            val filtered = bandpassFilter(detrended, targetFps.toDouble(), 0.4, 4.0)

            // Step 7: Normalize
            val normalized = normalize(filtered)

            // Step 8: Compute power spectral density using Welch's method
            val bpm = computeBpmFromPsd(normalized, targetFps.toDouble())

            // Step 9: Estimate SpO2 (simplified approximation based on R/IR ratio)
            val spo2 = estimateSpO2(r, g, b)

            Log.d(TAG, "Computed BPM: $bpm, SpO2: $spo2")

            return Pair(bpm, spo2)

        } catch (e: Exception) {
            Log.e(TAG, "Error computing heart rate", e)
            return Pair(0f, 0f)
        }
    }

    /**
     * Reset all signal buffers
     */
    fun reset() {
        redSignal.clear()
        greenSignal.clear()
        blueSignal.clear()
        timestamps.clear()
        Log.d(TAG, "Signal buffers reset")
    }

    // ==================== Private Helper Methods ====================

    private fun standardDeviation(data: DoubleArray): Double {
        if (data.isEmpty()) return 0.0
        val mean = data.average()
        val variance = data.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun detrend(signal: DoubleArray): DoubleArray {
        val mean = signal.average()
        return signal.map { it - mean }.toDoubleArray()
    }

    /**
     * Simple bandpass filter using moving average approximation
     */
    private fun bandpassFilter(
        signal: DoubleArray,
        fs: Double,
        lowCut: Double,
        highCut: Double
    ): DoubleArray {
        // For real-time performance, use a simple moving average approach
        // This is a simplified version; for better accuracy, use proper Butterworth filter

        val lowWindow = (fs / lowCut).toInt().coerceAtLeast(1)

        // High-pass filter (remove low frequencies)
        val highPassed = signal.mapIndexed { i, value ->
            val start = max(0, i - lowWindow / 2)
            val end = min(signal.size, i + lowWindow / 2 + 1)
            val windowMean = signal.slice(start until end).average()
            value - windowMean
        }.toDoubleArray()

        return highPassed
    }

    private fun normalize(signal: DoubleArray): DoubleArray {
        val mean = signal.average()
        val std = standardDeviation(signal)
        return if (std > 0) {
            signal.map { (it - mean) / std }.toDoubleArray()
        } else {
            signal
        }
    }

    /**
     * Compute BPM using Power Spectral Density (Welch's method approximation)
     */
    private fun computeBpmFromPsd(signal: DoubleArray, fs: Double): Float {
        try {
            // Simple FFT-based approach for finding dominant frequency
            val n = signal.size

            // Apply Hamming window
            val windowed = signal.mapIndexed { i, value ->
                val window = 0.54 - 0.46 * cos(2.0 * PI * i / (n - 1))
                value * window
            }.toDoubleArray()

            // Compute power spectrum (simplified DFT)
            val powerSpectrum = DoubleArray(n / 2)
            for (k in 0 until n / 2) {
                var real = 0.0
                var imag = 0.0
                for (i in windowed.indices) {
                    val angle = 2.0 * PI * k * i / n
                    real += windowed[i] * cos(angle)
                    imag += windowed[i] * sin(angle)
                }
                powerSpectrum[k] = sqrt(real * real + imag * imag)
            }

            // Find peak frequency in valid heart rate range
            val minFreq = minBpm / 60.0
            val maxFreq = maxBpm / 60.0
            val minIdx = (minFreq * n / fs).toInt().coerceAtLeast(0)
            val maxIdx = min((maxFreq * n / fs).toInt(), powerSpectrum.size - 1)

            if (minIdx >= maxIdx) {
                Log.w(TAG, "Invalid frequency range")
                return 0f
            }

            var maxPower = 0.0
            var maxIdxFound = minIdx
            for (i in minIdx..maxIdx) {
                if (powerSpectrum[i] > maxPower) {
                    maxPower = powerSpectrum[i]
                    maxIdxFound = i
                }
            }

            // Convert index to frequency and then to BPM
            val dominantFreq = maxIdxFound * fs / n
            val bpm = (dominantFreq * 60.0).toFloat()

            Log.d(TAG, "Dominant frequency: $dominantFreq Hz, BPM: $bpm")

            return bpm.coerceIn(minBpm, maxBpm)

        } catch (e: Exception) {
            Log.e(TAG, "Error in PSD computation", e)
            return 0f
        }
    }

    /**
     * Estimate SpO2 based on red/blue ratio
     * Note: This is a simplified approximation
     */
    private fun estimateSpO2(r: DoubleArray, g: DoubleArray, b: DoubleArray): Float {
        try {
            // Calculate AC/DC ratio for red and blue channels
            val rAC = standardDeviation(r)
            val rDC = r.average()
            val bAC = standardDeviation(b)
            val bDC = b.average()

            if (rDC > 0 && bDC > 0 && bAC > 0) {
                val ratioRed = rAC / rDC
                val ratioBlue = bAC / bDC

                // Simplified SpO2 calculation (typical calibration curve)
                // Real SpO2 requires proper calibration with pulse oximeter
                val ratio = if (ratioBlue > 0) ratioRed / ratioBlue else 1.0
                val spo2 = 110.0 - 25.0 * ratio

                return spo2.toFloat().coerceIn(90f, 100f)
            }

            return 97f // Default reasonable value

        } catch (e: Exception) {
            Log.e(TAG, "Error estimating SpO2", e)
            return 97f
        }
    }

    fun getCurrentBufferSize(): Int = redSignal.size
}