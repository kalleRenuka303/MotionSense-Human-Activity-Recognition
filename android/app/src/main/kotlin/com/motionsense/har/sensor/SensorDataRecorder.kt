package com.motionsense.har.sensor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorDataRecorder(private val context: Context) {

    private var activeWriter: BufferedWriter? = null
    private var activeFile: File? = null
    private var recordingStartTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _currentLabel = MutableStateFlow("WALKING")
    val currentLabel = _currentLabel.asStateFlow()

    private val _sampleCount = MutableStateFlow(0L)
    val sampleCount = _sampleCount.asStateFlow()

    private val _durationSec = MutableStateFlow(0L)
    val durationSec = _durationSec.asStateFlow()

    private val _lastSavedFile = MutableStateFlow<File?>(null)
    val lastSavedFile = _lastSavedFile.asStateFlow()

    companion object {
        private const val TAG = "SensorDataRecorder"
        private const val CSV_HEADER = "timestamp_ns,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,sampling_rate_hz,orientation,activity_label\n"
    }

    private fun getRecordingsDir(): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "sensor_recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Synchronized
    fun startRecording(activityLabel: String, orientation: String): File {
        if (_isRecording.value) {
            stopRecording()
        }

        val dir = getRecordingsDir()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanLabel = activityLabel.replace(" ", "_").lowercase(Locale.US)
        val cleanOrient = orientation.replace(" ", "_").lowercase(Locale.US)
        val file = File(dir, "motionsense_${cleanLabel}_${cleanOrient}_${timeStamp}.csv")

        val writer = BufferedWriter(FileWriter(file, false))
        writer.write(CSV_HEADER)
        activeWriter = writer
        activeFile = file
        recordingStartTime = System.currentTimeMillis()

        _currentLabel.value = activityLabel
        _sampleCount.value = 0L
        _durationSec.value = 0L
        _isRecording.value = true

        Log.d(TAG, "Started sensor recording: ${file.absolutePath} for label: $activityLabel")
        return file
    }

    @Synchronized
    fun recordSample(
        timestampNs: Long,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        samplingRateHz: Float,
        orientation: String,
        label: String
    ) {
        val writer = activeWriter ?: return
        try {
            val line = String.format(
                Locale.US,
                "%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.2f,%s,%s\n",
                timestampNs,
                accelX,
                accelY,
                accelZ,
                gyroX,
                gyroY,
                gyroZ,
                samplingRateHz,
                orientation,
                label
            )
            writer.write(line)
            val count = _sampleCount.value + 1
            _sampleCount.value = count
            _durationSec.value = (System.currentTimeMillis() - recordingStartTime) / 1000
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sensor sample: ${e.message}", e)
        }
    }

    @Synchronized
    fun stopRecording(): File? {
        if (!_isRecording.value) return activeFile
        val file = activeFile
        try {
            activeWriter?.flush()
            activeWriter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recording file: ${e.message}", e)
        } finally {
            activeWriter = null
            activeFile = null
            _isRecording.value = false
            _lastSavedFile.value = file
        }
        Log.d(TAG, "Stopped recording. Saved file: ${file?.absolutePath} (${_sampleCount.value} samples)")
        return file
    }

    fun getRecordings(): List<File> {
        val dir = getRecordingsDir()
        return dir.listFiles { f -> f.extension.equals("csv", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteRecording(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun createShareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MotionSense Sensor Recording: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
