package com.motionsense.har.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

data class SensorTelemetry(
    val totalAccX: Float = 0f,
    val totalAccY: Float = 0f,
    val totalAccZ: Float = 0f,
    val bodyAccX: Float = 0f,
    val bodyAccY: Float = 0f,
    val bodyAccZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val bufferFillPercent: Int = 0,
    val samplesCollected: Long = 0,
    val samplingRateHz: Float = 50.0f,
    val timestampJitterMs: Float = 0.0f,
    val missingSamples: Long = 0L,
    val lastTimestampNs: Long = 0L,
    val channelMin: FloatArray = FloatArray(9),
    val channelMax: FloatArray = FloatArray(9),
    val placement: PhonePlacement = PhonePlacement.FRONT_POCKET_PORTRAIT
)

data class SensorAvailability(
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasGravity: Boolean,
    val hasLinearAcceleration: Boolean,
    val accelerometerName: String,
    val gyroscopeName: String
)

class SensorCollector(
    private val context: Context,
    private val onWindowReady: (FloatArray) -> Unit,
    private val onTelemetryUpdate: (SensorTelemetry) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    companion object {
        private const val TAG = "SensorCollector"
        const val WINDOW_SIZE = 128
        const val WINDOW_STEP = 64 // 50% overlap
        const val NUM_CHANNELS = 9
        const val GRAVITY_STANDARD = 9.80665f // Standard gravity in m/s^2 for UCI HAR normalization
        const val SENSOR_RATE_MICROS = 20000 // 50 Hz = 20,000 microseconds
    }

    // Current placement mode (defaults to Front Pocket Portrait, can be changed at runtime)
    var placement: PhonePlacement = PhonePlacement.FRONT_POCKET_PORTRAIT

    // Exact 3rd-order Butterworth digital filter matching UCI HAR preprocessing (0.3 Hz cutoff)
    private val butterworthFilter = TriaxialButterworthFilter()

    // Latest raw sensor values (in normalized g and rad/s)
    private val rawTotalAcc = FloatArray(3)
    private val rawBodyAcc = FloatArray(3)
    private val rawGyro = FloatArray(3)
    private val rawGravity = FloatArray(3)

    // Jitter and frequency diagnostics tracking
    private var lastEventTimestampNs = 0L
    private val recentIntervalsNs = ArrayList<Long>(50)
    private var totalMissingSamples = 0L

    // Current window min/max tracking per channel (0..8)
    private val curChannelMin = FloatArray(NUM_CHANNELS) { Float.POSITIVE_INFINITY }
    private val curChannelMax = FloatArray(NUM_CHANNELS) { Float.NEGATIVE_INFINITY }

    // Window buffer: holds up to WINDOW_SIZE samples of 9 floats each
    private val sampleBuffer = ArrayList<FloatArray>(WINDOW_SIZE)
    private var totalSamplesProcessed = 0L

    // Last completed window preserved for diagnostics export
    @Volatile
    var lastCompletedWindow: FloatArray? = null
        private set

    @Volatile
    var isTracking = false
        private set

    fun getAvailability(): SensorAvailability {
        return SensorAvailability(
            hasAccelerometer = accelerometer != null,
            hasGyroscope = gyroscope != null,
            hasGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null,
            hasLinearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null,
            accelerometerName = accelerometer?.name ?: "Not Detected",
            gyroscopeName = gyroscope?.name ?: "Not Detected"
        )
    }

    fun start() {
        if (isTracking) return
        isTracking = true
        sampleBuffer.clear()
        totalSamplesProcessed = 0L
        butterworthFilter.reset()
        recentIntervalsNs.clear()
        lastEventTimestampNs = 0L
        totalMissingSamples = 0L
        resetMinMax()

        // Register accelerometer and gyroscope at 50 Hz
        accelerometer?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }

        Log.d(TAG, "Sensor collection started at 50Hz with placement: ${placement.title}")
    }

    fun stop() {
        if (!isTracking) return
        isTracking = false
        sensorManager.unregisterListener(this)
        sampleBuffer.clear()
        Log.d(TAG, "Sensor collection stopped.")
    }

    private fun resetMinMax() {
        for (i in 0 until NUM_CHANNELS) {
            curChannelMin[i] = Float.POSITIVE_INFINITY
            curChannelMax[i] = Float.NEGATIVE_INFINITY
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isTracking) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Track frequency and jitter
                if (lastEventTimestampNs > 0) {
                    val dt = event.timestamp - lastEventTimestampNs
                    if (dt > 40_000_000L) { // Gap > 40 ms (2x target interval of 20ms)
                        totalMissingSamples += (dt / 20_000_000L) - 1
                    }
                    recentIntervalsNs.add(dt)
                    if (recentIntervalsNs.size > 50) {
                        recentIntervalsNs.removeAt(0)
                    }
                }
                lastEventTimestampNs = event.timestamp

                // Total acceleration in m/s^2 -> normalize to 'g' units to match UCI HAR
                rawTotalAcc[0] = event.values[0] / GRAVITY_STANDARD
                rawTotalAcc[1] = event.values[1] / GRAVITY_STANDARD
                rawTotalAcc[2] = event.values[2] / GRAVITY_STANDARD

                // Apply exact 3rd-order Butterworth low-pass filter (0.3 Hz cutoff) to extract gravity
                val grav = butterworthFilter.filterGravity(rawTotalAcc)
                rawGravity[0] = grav[0]
                rawGravity[1] = grav[1]
                rawGravity[2] = grav[2]

                // Body acceleration = total acceleration - gravity
                rawBodyAcc[0] = rawTotalAcc[0] - rawGravity[0]
                rawBodyAcc[1] = rawTotalAcc[1] - rawGravity[1]
                rawBodyAcc[2] = rawTotalAcc[2] - rawGravity[2]
            }

            Sensor.TYPE_GYROSCOPE -> {
                // Gyroscope in rad/s directly matches UCI HAR units
                rawGyro[0] = event.values[0]
                rawGyro[1] = event.values[1]
                rawGyro[2] = event.values[2]

                // Synchronize sample on gyroscope update
                recordSample()
            }
        }
    }

    private fun recordSample() {
        // Apply coordinate alignment transformation based on selected phone placement
        val alignedTotalAcc = placement.alignVector(rawTotalAcc)
        val alignedBodyAcc = placement.alignVector(rawBodyAcc)
        val alignedGyro = placement.alignVector(rawGyro)

        val sample = FloatArray(NUM_CHANNELS)
        // 0..2: total_acc (UCI model X, Y, Z)
        sample[0] = alignedTotalAcc[0]
        sample[1] = alignedTotalAcc[1]
        sample[2] = alignedTotalAcc[2]

        // 3..5: body_acc (UCI model X, Y, Z)
        sample[3] = alignedBodyAcc[0]
        sample[4] = alignedBodyAcc[1]
        sample[5] = alignedBodyAcc[2]

        // 6..8: body_gyro (UCI model X, Y, Z)
        sample[6] = alignedGyro[0]
        sample[7] = alignedGyro[1]
        sample[8] = alignedGyro[2]

        // Track channel min/max
        for (i in 0 until NUM_CHANNELS) {
            if (sample[i] < curChannelMin[i]) curChannelMin[i] = sample[i]
            if (sample[i] > curChannelMax[i]) curChannelMax[i] = sample[i]
        }

        sampleBuffer.add(sample)
        totalSamplesProcessed++

        // Compute real-time frequency and jitter
        var avgHz = 50.0f
        var jitterMs = 0.0f
        if (recentIntervalsNs.isNotEmpty()) {
            val avgIntervalNs = recentIntervalsNs.average()
            if (avgIntervalNs > 0) {
                avgHz = (1_000_000_000.0 / avgIntervalNs).toFloat()
            }
            var sumSqDiff = 0.0
            for (dt in recentIntervalsNs) {
                val diff = (dt - avgIntervalNs) / 1_000_000.0
                sumSqDiff += diff * diff
            }
            jitterMs = sqrt(sumSqDiff / recentIntervalsNs.size).toFloat()
        }

        val fillPercent = (sampleBuffer.size * 100) / WINDOW_SIZE
        onTelemetryUpdate(
            SensorTelemetry(
                totalAccX = alignedTotalAcc[0],
                totalAccY = alignedTotalAcc[1],
                totalAccZ = alignedTotalAcc[2],
                bodyAccX = alignedBodyAcc[0],
                bodyAccY = alignedBodyAcc[1],
                bodyAccZ = alignedBodyAcc[2],
                gyroX = alignedGyro[0],
                gyroY = alignedGyro[1],
                gyroZ = alignedGyro[2],
                gravityX = rawGravity[0],
                gravityY = rawGravity[1],
                gravityZ = rawGravity[2],
                bufferFillPercent = fillPercent,
                samplesCollected = totalSamplesProcessed,
                samplingRateHz = avgHz,
                timestampJitterMs = jitterMs,
                missingSamples = totalMissingSamples,
                lastTimestampNs = lastEventTimestampNs,
                channelMin = curChannelMin.clone(),
                channelMax = curChannelMax.clone(),
                placement = placement
            )
        )

        // When window reaches 128 timesteps, emit flattened [128, 9] array
        if (sampleBuffer.size >= WINDOW_SIZE) {
            val flattenedWindow = FloatArray(WINDOW_SIZE * NUM_CHANNELS)
            for (i in 0 until WINDOW_SIZE) {
                val row = sampleBuffer[i]
                System.arraycopy(row, 0, flattenedWindow, i * NUM_CHANNELS, NUM_CHANNELS)
            }
            lastCompletedWindow = flattenedWindow.clone()
            onWindowReady(flattenedWindow)

            // Reset channel min/max for next window
            resetMinMax()

            // 50% sliding window: remove first 64 samples
            sampleBuffer.subList(0, WINDOW_STEP).clear()
        }
    }

    /**
     * Exports the last completed 128x9 window to local storage as JSON for Python parity comparison.
     */
    fun exportDiagnosticWindowToFile(): String? {
        val window = lastCompletedWindow ?: return null
        return try {
            val dir = File(context.getExternalFilesDir(null), "diagnostics")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val file = File(dir, "sensor_window_${timestamp}.json")

            val json = JSONObject()
            json.put("timestamp", timestamp)
            json.put("placement", placement.name)
            json.put("window_size", WINDOW_SIZE)
            json.put("num_channels", NUM_CHANNELS)

            val windowArr = JSONArray()
            for (t in 0 until WINDOW_SIZE) {
                val sampleArr = JSONArray()
                for (c in 0 until NUM_CHANNELS) {
                    sampleArr.put(window[t * NUM_CHANNELS + c].toDouble())
                }
                windowArr.put(sampleArr)
            }
            json.put("data", windowArr)

            file.writeText(json.toString(2))
            Log.d(TAG, "Saved diagnostic window to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save diagnostic window: ${e.message}", e)
            null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for inertial sensors
    }
}
