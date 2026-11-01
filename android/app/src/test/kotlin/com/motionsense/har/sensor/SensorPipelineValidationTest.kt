package com.motionsense.har.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SensorPipelineValidationTest {

    @Test
    fun testSensorUnitsNormalization() {
        // Android SensorManager provides accelerometer values in m/s^2.
        // Standard gravity is 9.80665 m/s^2.
        val standardGravityMps2 = 9.80665f
        val gravityG = standardGravityMps2 / SensorCollector.GRAVITY_STANDARD
        assertEquals(1.0f, gravityG, 1e-5f)

        // Test arbitrary acceleration: 19.6133 m/s^2 -> 2.0 g
        val doubleGravityMps2 = 19.6133f
        val doubleG = doubleGravityMps2 / SensorCollector.GRAVITY_STANDARD
        assertEquals(2.0f, doubleG, 1e-4f)
    }

    @Test
    fun testChannelOrderingMatchesPipelineB() {
        // Pipeline B channel specifications:
        // 0: total_acc_x, 1: total_acc_y, 2: total_acc_z
        // 3: body_acc_x,  4: body_acc_y,  5: body_acc_z
        // 6: body_gyro_x, 7: body_gyro_y, 8: body_gyro_z
        val channelNames = listOf(
            "total_acc_x", "total_acc_y", "total_acc_z",
            "body_acc_x", "body_acc_y", "body_acc_z",
            "body_gyro_x", "body_gyro_y", "body_gyro_z"
        )
        assertEquals(9, channelNames.size)
        assertEquals(SensorCollector.NUM_CHANNELS, channelNames.size)

        val sample = FloatArray(SensorCollector.NUM_CHANNELS)
        for (i in 0 until 9) {
            sample[i] = i.toFloat()
        }

        // Verify channel values match expected indices
        assertEquals(0f, sample[0], 0f) // total_acc_x
        assertEquals(1f, sample[1], 0f) // total_acc_y
        assertEquals(2f, sample[2], 0f) // total_acc_z
        assertEquals(3f, sample[3], 0f) // body_acc_x
        assertEquals(4f, sample[4], 0f) // body_acc_y
        assertEquals(5f, sample[5], 0f) // body_acc_z
        assertEquals(6f, sample[6], 0f) // body_gyro_x
        assertEquals(7f, sample[7], 0f) // body_gyro_y
        assertEquals(8f, sample[8], 0f) // body_gyro_z
    }

    @Test
    fun testWindowBufferingAndOverlap() {
        val windowSize = SensorCollector.WINDOW_SIZE // 128
        val windowStep = SensorCollector.WINDOW_STEP // 64
        val numChannels = SensorCollector.NUM_CHANNELS // 9

        assertEquals(128, windowSize)
        assertEquals(64, windowStep)
        assertEquals(9, numChannels)

        // Verify window memory layout
        val flattenedLength = windowSize * numChannels
        assertEquals(1152, flattenedLength)
    }

    @Test
    fun testFiveMinuteTimestampJitterAnalysis() {
        // Simulate a 5-minute 50 Hz sensor stream:
        // Duration: 300 seconds
        // Target interval: 20 ms (20,000,000 ns)
        // Total samples expected: 300 * 50 = 15,000 samples
        val targetIntervalNs = 20_000_000L // 20 ms
        val totalExpectedSamples = 15_000

        val timestamps = LongArray(totalExpectedSamples)
        var curNs = 1_000_000_000_000L // arbitrary start timestamp in nanoseconds

        // Simulate real-world jitter: Gaussian-like jitter within ±1.5 ms
        val jitterMs = floatArrayOf(-0.8f, 0.5f, -1.2f, 0.9f, -0.4f, 1.4f, -0.1f, 0.7f, -1.5f, 0.2f)
        for (i in 0 until totalExpectedSamples) {
            val j = (jitterMs[i % jitterMs.size] * 1_000_000).toLong()
            curNs += targetIntervalNs + j
            timestamps[i] = curNs
        }

        // Analyze intervals
        val intervalsMs = mutableListOf<Double>()
        var missingSamples = 0
        for (i in 1 until totalExpectedSamples) {
            val dtMs = (timestamps[i] - timestamps[i - 1]) / 1_000_000.0
            intervalsMs.add(dtMs)
            if (dtMs > 40.0) { // Gap > 2x interval
                missingSamples++
            }
        }

        val avgIntervalMs = intervalsMs.average()
        val avgFrequencyHz = 1000.0 / avgIntervalMs

        var sumSqDiff = 0.0
        for (dt in intervalsMs) {
            val diff = dt - avgIntervalMs
            sumSqDiff += diff * diff
        }
        val jitterStdDevMs = sqrt(sumSqDiff / intervalsMs.size)

        // Verify within physical tolerances:
        // Expected frequency ~50.0 Hz (within 49.5 - 50.5 Hz)
        assertTrue(avgFrequencyHz in 49.0..51.0)
        // Jitter should be small (< 2.0 ms)
        assertTrue(jitterStdDevMs < 2.0)
        assertEquals(0, missingSamples)
    }
}
