package com.motionsense.har.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.motionsense.har.MainActivity
import com.motionsense.har.data.db.MotionSenseDatabase
import com.motionsense.har.data.model.ActivityRecord
import com.motionsense.har.data.model.ActivityType
import com.motionsense.har.ml.ClassificationResult
import com.motionsense.har.ml.MotionClassifier
import com.motionsense.har.sensor.SensorCollector
import com.motionsense.har.sensor.SensorTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MotionTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorCollector: SensorCollector
    private lateinit var classifier: MotionClassifier
    private lateinit var database: MotionSenseDatabase
    private var wakeLock: PowerManager.WakeLock? = null

    private val _currentActivity = MutableStateFlow(ActivityType.UNKNOWN)
    val currentActivity = _currentActivity.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence = _confidence.asStateFlow()

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry = _telemetry.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _trackingDurationSec = MutableStateFlow(0L)
    val trackingDurationSec = _trackingDurationSec.asStateFlow()

    private val _latestResult = MutableStateFlow<ClassificationResult?>(null)
    val latestResult = _latestResult.asStateFlow()

    private var selectedModel = MotionClassifier.MODEL_BINARY_WALKING
    private var trackingStartTime = 0L

    companion object {
        const val CHANNEL_ID = "motionsense_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): MotionTrackingService = this@MotionTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        database = MotionSenseDatabase.getInstance(this)
        classifier = MotionClassifier(this)

        sensorCollector = SensorCollector(
            context = this,
            onWindowReady = { windowData ->
                onWindowClassified(windowData)
            },
            onTelemetryUpdate = { telem ->
                _telemetry.value = telem
            }
        )

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MotionSense:TrackingWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    fun setModel(model: String) {
        selectedModel = model
    }

    fun setPlacement(placement: com.motionsense.har.sensor.PhonePlacement) {
        sensorCollector.placement = placement
    }

    fun exportDiagnosticWindow(): String? {
        return sensorCollector.exportDiagnosticWindowToFile()
    }

    val isRecordingFlow get() = sensorCollector.recorder.isRecording
    val recordingSampleCount get() = sensorCollector.recorder.sampleCount
    val recordingDurationSec get() = sensorCollector.recorder.durationSec
    val recordingLabel get() = sensorCollector.recorder.currentLabel
    val lastSavedRecording get() = sensorCollector.recorder.lastSavedFile

    fun startSensorRecording(label: String): java.io.File {
        return sensorCollector.recorder.startRecording(label, sensorCollector.placement.title)
    }

    fun stopSensorRecording(): java.io.File? {
        return sensorCollector.recorder.stopRecording()
    }

    fun getRecordings(): List<java.io.File> {
        return sensorCollector.recorder.getRecordings()
    }

    fun deleteRecording(file: java.io.File): Boolean {
        return sensorCollector.recorder.deleteRecording(file)
    }

    fun createShareIntent(file: java.io.File): Intent {
        return sensorCollector.recorder.createShareIntent(file)
    }

    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        trackingStartTime = System.currentTimeMillis()

        wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24hr max fallback
        sensorCollector.start()

        startForeground(NOTIFICATION_ID, buildNotification("Initializing sensor analysis..."))

        // Duration timer loop
        serviceScope.launch {
            while (_isTracking.value) {
                kotlinx.coroutines.delay(1000)
                _trackingDurationSec.value = (System.currentTimeMillis() - trackingStartTime) / 1000
            }
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) return
        _isTracking.value = false

        sensorCollector.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onWindowClassified(windowData: FloatArray) {
        val result = classifier.classifyWindow(
            windowData = windowData,
            modelType = selectedModel
        )

        _latestResult.value = result
        _currentActivity.value = result.activityType
        _confidence.value = result.confidence

        updateNotification(result)

        // Persist activity record if definitive
        if (!result.isUncertain && result.activityType != ActivityType.UNCERTAIN && result.activityType != ActivityType.UNKNOWN) {
            serviceScope.launch {
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                database.insertRecord(
                    ActivityRecord(
                        timestamp = now,
                        activityName = result.activityType.name,
                        confidence = result.confidence,
                        durationSeconds = 2, // 2.56s window with 1.28s step
                        modelUsed = result.modelUsed,
                        dateString = dateFormat.format(Date(now))
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MotionSense Active Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time human activity recognition status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MotionTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MotionSense Activity Recognition")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(result: ClassificationResult) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "Detected: ${result.activityType.displayName} (${(result.confidence * 100).toInt()}%)"
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        stopTracking()
        classifier.close()
        serviceScope.cancel()
        super.onDestroy()
    }
}
