package com.motionsense.har.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motionsense.har.data.db.MotionSenseDatabase
import com.motionsense.har.data.model.ActivityRecord
import com.motionsense.har.data.model.ActivityType
import com.motionsense.har.ml.MotionClassifier
import com.motionsense.har.sensor.SensorAvailability
import com.motionsense.har.sensor.SensorCollector
import com.motionsense.har.sensor.SensorTelemetry
import com.motionsense.har.service.MotionTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailySummary(
    val walkingSec: Long = 0,
    val stairsSec: Long = 0,
    val sittingSec: Long = 0,
    val standingSec: Long = 0,
    val layingSec: Long = 0,
    val totalActiveSec: Long = 0,
    val totalSedentarySec: Long = 0
)

class MotionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MotionSenseDatabase.getInstance(application)
    private var trackingService: MotionTrackingService? = null
    private var isBound = false

    val records: StateFlow<List<ActivityRecord>> = database.recordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentActivity = MutableStateFlow(ActivityType.STANDING)
    val currentActivity = _currentActivity.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence = _confidence.asStateFlow()

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry = _telemetry.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _trackingDurationSec = MutableStateFlow(0L)
    val trackingDurationSec = _trackingDurationSec.asStateFlow()

    private val _selectedModel = MutableStateFlow(MotionClassifier.MODEL_1D_CNN)
    val selectedModel = _selectedModel.asStateFlow()

    private val _phonePlacement = MutableStateFlow(com.motionsense.har.sensor.PhonePlacement.FRONT_POCKET_PORTRAIT)
    val phonePlacement = _phonePlacement.asStateFlow()

    private val _latestResult = MutableStateFlow<com.motionsense.har.ml.ClassificationResult?>(null)
    val latestResult = _latestResult.asStateFlow()

    private val _sensorAvailability = MutableStateFlow<SensorAvailability?>(null)
    val sensorAvailability = _sensorAvailability.asStateFlow()

    private val _dailySummary = MutableStateFlow(DailySummary())
    val dailySummary = _dailySummary.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MotionTrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true

            trackingService?.let { s ->
                s.setPlacement(_phonePlacement.value)
                s.setModel(_selectedModel.value)

                viewModelScope.launch {
                    s.currentActivity.collect { _currentActivity.value = it }
                }
                viewModelScope.launch {
                    s.confidence.collect { _confidence.value = it }
                }
                viewModelScope.launch {
                    s.telemetry.collect { _telemetry.value = it }
                }
                viewModelScope.launch {
                    s.isTracking.collect { _isTracking.value = it }
                }
                viewModelScope.launch {
                    s.trackingDurationSec.collect { _trackingDurationSec.value = it }
                }
                viewModelScope.launch {
                    s.latestResult.collect { _latestResult.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
        }
    }

    init {
        checkSensors()
        bindTrackingService()
        observeRecordsForSummary()
    }

    private fun checkSensors() {
        val dummy = SensorCollector(getApplication(), {}, {})
        _sensorAvailability.value = dummy.getAvailability()
    }

    private fun bindTrackingService() {
        val intent = Intent(getApplication(), MotionTrackingService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startTracking() {
        val intent = Intent(getApplication(), MotionTrackingService::class.java).apply {
            action = MotionTrackingService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
        trackingService?.setModel(_selectedModel.value)
        trackingService?.startTracking()
    }

    fun stopTracking() {
        trackingService?.stopTracking()
        val intent = Intent(getApplication(), MotionTrackingService::class.java).apply {
            action = MotionTrackingService.ACTION_STOP
        }
        getApplication<Application>().stopService(intent)
    }

    fun setModel(model: String) {
        _selectedModel.value = model
        trackingService?.setModel(model)
    }

    fun setPlacement(placement: com.motionsense.har.sensor.PhonePlacement) {
        _phonePlacement.value = placement
        trackingService?.setPlacement(placement)
    }

    fun exportDiagnosticWindow(): String? {
        return trackingService?.exportDiagnosticWindow()
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.clearHistory()
        }
    }

    private fun observeRecordsForSummary() {
        viewModelScope.launch {
            records.collect { list ->
                var walking = 0L
                var stairs = 0L
                var sitting = 0L
                var standing = 0L
                var laying = 0L

                for (r in list) {
                    when (r.activityName) {
                        "WALKING" -> walking += r.durationSeconds
                        "WALKING_UPSTAIRS", "WALKING_DOWNSTAIRS" -> stairs += r.durationSeconds
                        "SITTING" -> sitting += r.durationSeconds
                        "STANDING" -> standing += r.durationSeconds
                        "LAYING" -> laying += r.durationSeconds
                    }
                }

                _dailySummary.value = DailySummary(
                    walkingSec = walking,
                    stairsSec = stairs,
                    sittingSec = sitting,
                    standingSec = standing,
                    layingSec = laying,
                    totalActiveSec = walking + stairs,
                    totalSedentarySec = sitting + standing + laying
                )
            }
        }
    }

    override fun onCleared() {
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
        super.onCleared()
    }
}
