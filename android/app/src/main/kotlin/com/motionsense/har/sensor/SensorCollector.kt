package com.motionsense.har.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

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
    val bufferFillPercent: Int = 0,
    val samplesCollected: Long = 0
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
    private val linearAcceleration: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    companion object {
        private const val TAG = "SensorCollector"
        const val WINDOW_SIZE = 128
        const val WINDOW_STEP = 64 // 50% overlap
        const val NUM_CHANNELS = 9
        const val GRAVITY_STANDARD = 9.80665f // Standard gravity in m/s^2 for UCI HAR normalization
        const val SENSOR_RATE_MICROS = 20000 // 50 Hz = 20,000 microseconds
    }

    // Latest instantaneous sensor readings
    private var curTotalAcc = FloatArray(3)
    private var curBodyAcc = FloatArray(3)
    private var curGyro = FloatArray(3)
    private var curGravity = FloatArray(3)

    // Window buffer: holds up to WINDOW_SIZE samples of 9 floats each
    private val sampleBuffer = ArrayList<FloatArray>(WINDOW_SIZE)
    private var totalSamplesProcessed = 0L

    @Volatile
    var isTracking = false
        private set

    fun getAvailability(): SensorAvailability {
        return SensorAvailability(
            hasAccelerometer = accelerometer != null,
            hasGyroscope = gyroscope != null,
            hasGravity = gravitySensor != null,
            hasLinearAcceleration = linearAcceleration != null,
            accelerometerName = accelerometer?.name ?: "Not Detected",
            gyroscopeName = gyroscope?.name ?: "Not Detected"
        )
    }

    fun start() {
        if (isTracking) return
        isTracking = true
        sampleBuffer.clear()
        totalSamplesProcessed = 0L

        // Register sensors at 50 Hz
        accelerometer?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }
        linearAcceleration?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SENSOR_RATE_MICROS)
        }

        Log.d(TAG, "Sensor collection started at 50Hz.")
    }

    fun stop() {
        if (!isTracking) return
        isTracking = false
        sensorManager.unregisterListener(this)
        sampleBuffer.clear()
        Log.d(TAG, "Sensor collection stopped.")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isTracking) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Total acceleration in m/s^2 -> normalize to 'g' units to match UCI HAR
                curTotalAcc[0] = event.values[0] / GRAVITY_STANDARD
                curTotalAcc[1] = event.values[1] / GRAVITY_STANDARD
                curTotalAcc[2] = event.values[2] / GRAVITY_STANDARD

                // If linear acceleration sensor is absent, compute body acc using low-pass gravity filter
                if (linearAcceleration == null) {
                    val alpha = 0.8f
                    curGravity[0] = alpha * curGravity[0] + (1 - alpha) * curTotalAcc[0]
                    curGravity[1] = alpha * curGravity[1] + (1 - alpha) * curTotalAcc[1]
                    curGravity[2] = alpha * curGravity[2] + (1 - alpha) * curTotalAcc[2]

                    curBodyAcc[0] = curTotalAcc[0] - curGravity[0]
                    curBodyAcc[1] = curTotalAcc[1] - curGravity[1]
                    curBodyAcc[2] = curTotalAcc[2] - curGravity[2]
                }
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Direct body acceleration from Android sensor fusion
                curBodyAcc[0] = event.values[0] / GRAVITY_STANDARD
                curBodyAcc[1] = event.values[1] / GRAVITY_STANDARD
                curBodyAcc[2] = event.values[2] / GRAVITY_STANDARD
            }

            Sensor.TYPE_GRAVITY -> {
                curGravity[0] = event.values[0] / GRAVITY_STANDARD
                curGravity[1] = event.values[1] / GRAVITY_STANDARD
                curGravity[2] = event.values[2] / GRAVITY_STANDARD
            }

            Sensor.TYPE_GYROSCOPE -> {
                // Gyroscope in rad/s directly matches UCI HAR units
                curGyro[0] = event.values[0]
                curGyro[1] = event.values[1]
                curGyro[2] = event.values[2]

                // Synchronize sample on gyroscope update (or accelerometer)
                recordSample()
            }
        }
    }

    private fun recordSample() {
        val sample = FloatArray(NUM_CHANNELS)
        // Channel ordering matching Pipeline B:
        // 0..2: total_acc_x, total_acc_y, total_acc_z
        sample[0] = curTotalAcc[0]
        sample[1] = curTotalAcc[1]
        sample[2] = curTotalAcc[2]

        // 3..5: body_acc_x, body_acc_y, body_acc_z
        sample[3] = curBodyAcc[0]
        sample[4] = curBodyAcc[1]
        sample[5] = curBodyAcc[2]

        // 6..8: body_gyro_x, body_gyro_y, body_gyro_z
        sample[6] = curGyro[0]
        sample[7] = curGyro[1]
        sample[8] = curGyro[2]

        sampleBuffer.add(sample)
        totalSamplesProcessed++

        val fillPercent = (sampleBuffer.size * 100) / WINDOW_SIZE
        onTelemetryUpdate(
            SensorTelemetry(
                totalAccX = curTotalAcc[0],
                totalAccY = curTotalAcc[1],
                totalAccZ = curTotalAcc[2],
                bodyAccX = curBodyAcc[0],
                bodyAccY = curBodyAcc[1],
                bodyAccZ = curBodyAcc[2],
                gyroX = curGyro[0],
                gyroY = curGyro[1],
                gyroZ = curGyro[2],
                bufferFillPercent = fillPercent,
                samplesCollected = totalSamplesProcessed
            )
        )

        // When window reaches 128 timesteps, emit flattened [128, 9] array
        if (sampleBuffer.size >= WINDOW_SIZE) {
            val flattenedWindow = FloatArray(WINDOW_SIZE * NUM_CHANNELS)
            for (i in 0 until WINDOW_SIZE) {
                val row = sampleBuffer[i]
                System.arraycopy(row, 0, flattenedWindow, i * NUM_CHANNELS, NUM_CHANNELS)
            }
            onWindowReady(flattenedWindow)

            // 50% sliding window: remove first 64 samples
            sampleBuffer.subList(0, WINDOW_STEP).clear()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for inertial sensors
    }
}
