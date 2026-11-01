package com.motionsense.har.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionsense.har.data.model.ActivityType
import com.motionsense.har.ml.MotionClassifier
import com.motionsense.har.sensor.PhonePlacement
import com.motionsense.har.ui.theme.*
import com.motionsense.har.ui.viewmodel.MotionViewModel
import java.io.File

@Composable
fun HomeScreen(viewModel: MotionViewModel) {
    val context = LocalContext.current
    val currentActivity by viewModel.currentActivity.collectAsState()
    val confidence by viewModel.confidence.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val durationSec by viewModel.trackingDurationSec.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val placement by viewModel.phonePlacement.collectAsState()
    val latestResult by viewModel.latestResult.collectAsState()

    // Sensor Recorder States
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingCount by viewModel.recordingSampleCount.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordingLabel by viewModel.recordingLabel.collectAsState()
    val savedRecordings by viewModel.savedRecordingsList.collectAsState()

    var showDiagnostics by remember { mutableStateOf(false) }
    var showRecordingsList by remember { mutableStateOf(false) }
    var selectedGroundTruthLabel by remember { mutableStateOf("WALKING") }

    val activityColor by animateColorAsState(
        targetValue = when (currentActivity) {
            ActivityType.WALKING -> ActivityWalking
            ActivityType.NOT_WALKING -> ActivityNotWalking
            ActivityType.WALKING_UPSTAIRS -> ActivityWalkingUpstairs
            ActivityType.WALKING_DOWNSTAIRS -> ActivityWalkingDownstairs
            ActivityType.SITTING -> ActivitySitting
            ActivityType.STANDING -> ActivityStanding
            ActivityType.LAYING -> ActivityLaying
            ActivityType.UNKNOWN -> ActivityUnknown
            ActivityType.UNCERTAIN -> ActivityUncertain
        },
        label = "activityColor"
    )

    val activityIcon: ImageVector = when (currentActivity) {
        ActivityType.WALKING -> Icons.Default.DirectionsWalk
        ActivityType.NOT_WALKING -> Icons.Default.Accessibility
        ActivityType.WALKING_UPSTAIRS -> Icons.Default.DirectionsRun
        ActivityType.WALKING_DOWNSTAIRS -> Icons.Default.DirectionsWalk
        ActivityType.SITTING -> Icons.Default.AirlineSeatReclineNormal
        ActivityType.STANDING -> Icons.Default.AccessibilityNew
        ActivityType.LAYING -> Icons.Default.Hotel
        ActivityType.UNKNOWN -> Icons.Default.Sync
        ActivityType.UNCERTAIN -> Icons.Default.HelpOutline
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Live Activity Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Activity Icon Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(activityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(activityColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activityIcon,
                            contentDescription = currentActivity.displayName,
                            tint = activityColor,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                // Activity Name & Category
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentActivity.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = activityColor
                    )
                    Text(
                        text = "${currentActivity.category} Motion • ${placement.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Confidence Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Model Confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = activityColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = activityColor,
                        trackColor = activityColor.copy(alpha = 0.2f),
                    )
                }

                // Tracking Duration & Buffer Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Session Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sampling Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.1f", telemetry.samplingRateHz)} Hz",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Start / Stop Tracking Action Buttons
        if (!isTracking) {
            Button(
                onClick = { viewModel.startTracking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActivityWalking)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START REAL-TIME TRACKING",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = { viewModel.stopTracking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP TRACKING",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Ground-Truth Sensor Data Recorder Card (Phase 1 Requirement)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sensor Data Recorder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isRecording) "RECORDING" else "IDLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Record real 50 Hz accelerometer & gyroscope CSVs with ground-truth activity labels for physical validation:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Activity Label Selector Chips
                Text(
                    text = "Select Ground-Truth Label:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                val labels = listOf("WALKING", "SITTING", "STANDING", "WALKING_UPSTAIRS", "WALKING_DOWNSTAIRS", "LAYING")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        labels.take(3).forEach { lbl ->
                            FilterChip(
                                selected = selectedGroundTruthLabel == lbl,
                                onClick = {
                                    if (!isRecording) selectedGroundTruthLabel = lbl
                                },
                                label = { Text(lbl.replace("_", " "), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        labels.drop(3).forEach { lbl ->
                            FilterChip(
                                selected = selectedGroundTruthLabel == lbl,
                                onClick = {
                                    if (!isRecording) selectedGroundTruthLabel = lbl
                                },
                                label = { Text(lbl.replace("_", " "), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Recording Status / Button
                if (isRecording) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Recording: $recordingLabel • $recordingCount samples • ${recordingDurationSec}s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val saved = viewModel.stopSensorRecording()
                            Toast.makeText(context, "Saved recording: ${saved?.name}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP & SAVE CSV RECORDING")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.startSensorRecording(selectedGroundTruthLabel)
                            Toast.makeText(context, "Started recording: $selectedGroundTruthLabel", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START RECORDING ($selectedGroundTruthLabel)")
                    }
                }

                // Saved Recordings List Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Recordings (${savedRecordings.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showRecordingsList = !showRecordingsList }) {
                        Text(if (showRecordingsList) "Hide" else "View All")
                    }
                }

                if (showRecordingsList) {
                    if (savedRecordings.isEmpty()) {
                        Text(
                            text = "No saved sensor recordings yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedRecordings.take(5).forEach { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${file.length() / 1024} KB",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val intent = viewModel.createShareIntent(file)
                                                    if (intent != null) {
                                                        context.startActivity(Intent.createChooser(intent, "Share Sensor CSV"))
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteRecording(file)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Phone Placement Selector (Root Cause Fix for Walking -> Laying)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phone Placement (Orientation)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ActivityWalking,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Select your current phone carrying style for proper coordinate alignment:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = placement == PhonePlacement.HANDHELD_PORTRAIT,
                        onClick = { viewModel.setPlacement(PhonePlacement.HANDHELD_PORTRAIT) },
                        label = { Text("Handheld", fontSize = 12.sp) },
                        leadingIcon = if (placement == PhonePlacement.HANDHELD_PORTRAIT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = placement == PhonePlacement.FRONT_POCKET_PORTRAIT,
                        onClick = { viewModel.setPlacement(PhonePlacement.FRONT_POCKET_PORTRAIT) },
                        label = { Text("Pocket", fontSize = 12.sp) },
                        leadingIcon = if (placement == PhonePlacement.FRONT_POCKET_PORTRAIT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = placement == PhonePlacement.WAIST_HOLSTER_LANDSCAPE,
                        onClick = { viewModel.setPlacement(PhonePlacement.WAIST_HOLSTER_LANDSCAPE) },
                        label = { Text("Waist Belt", fontSize = 12.sp) },
                        leadingIcon = if (placement == PhonePlacement.WAIST_HOLSTER_LANDSCAPE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Model Selector Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inference Neural Network",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "4 AVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary Recommended Models
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedModel == MotionClassifier.MODEL_BINARY_WALKING,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_BINARY_WALKING) },
                        label = { Text("Binary Invariant (99.86% • Handheld Recommended)", fontSize = 12.sp) },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_BINARY_WALKING) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedModel == MotionClassifier.MODEL_3_CLASS,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_3_CLASS) },
                        label = { Text("3-Class: Walk / Sit / Stand (87.43% • Experimental)", fontSize = 12.sp) },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_3_CLASS) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedModel == MotionClassifier.MODEL_1D_CNN,
                            onClick = { viewModel.setModel(MotionClassifier.MODEL_1D_CNN) },
                            label = { Text("6-Class 1D CNN", fontSize = 11.sp) },
                            leadingIcon = if (selectedModel == MotionClassifier.MODEL_1D_CNN) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedModel == MotionClassifier.MODEL_CNN_LSTM,
                            onClick = { viewModel.setModel(MotionClassifier.MODEL_CNN_LSTM) },
                            label = { Text("6-Class CNN-LSTM", fontSize = 11.sp) },
                            leadingIcon = if (selectedModel == MotionClassifier.MODEL_CNN_LSTM) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Toggle Sensor Diagnostics Mode
        OutlinedButton(
            onClick = { showDiagnostics = !showDiagnostics },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                if (showDiagnostics) Icons.Default.VisibilityOff else Icons.Default.BugReport,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showDiagnostics) "Hide Sensor Diagnostics" else "Show Sensor & ML Diagnostics")
        }

        // Complete Sensor & ML Diagnostics Panel
        if (showDiagnostics) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🔬 SENSOR & ML DIAGNOSTICS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Timing & Jitter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sampling: ${String.format("%.1f", telemetry.samplingRateHz)} Hz", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "Jitter: ${String.format("%.2f", telemetry.timestampJitterMs)} ms", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "Missing: ${telemetry.missingSamples}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    HorizontalDivider()

                    // Gravity & Body Acceleration (Butterworth 0.3 Hz output)
                    Text(text = "Extracted Gravity (0.3 Hz Butterworth):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("gx: ${String.format("%+.2f", telemetry.gravityX)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("gy: ${String.format("%+.2f", telemetry.gravityY)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("gz: ${String.format("%+.2f", telemetry.gravityZ)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    Text(text = "Body Acceleration (Total - Gravity):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("bx: ${String.format("%+.2f", telemetry.bodyAccX)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("by: ${String.format("%+.2f", telemetry.bodyAccY)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("bz: ${String.format("%+.2f", telemetry.bodyAccZ)}g", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    HorizontalDivider()

                    // Model Input Tensor Channels (0..8) Min / Max
                    Text(text = "Model Input Tensor [1, 128, 9] Min / Max:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 0 until 9) {
                            val name = when (i) {
                                0 -> "total_acc_x"
                                1 -> "total_acc_y"
                                2 -> "total_acc_z"
                                3 -> "body_acc_x"
                                4 -> "body_acc_y"
                                5 -> "body_acc_z"
                                6 -> "gyro_x"
                                7 -> "gyro_y"
                                else -> "gyro_z"
                            }
                            val minV = telemetry.channelMin.getOrNull(i) ?: 0f
                            val maxV = telemetry.channelMax.getOrNull(i) ?: 0f
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ch $i ($name):", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    "[${if (minV == Float.POSITIVE_INFINITY) "—" else String.format("%+.2f", minV)} .. ${if (maxV == Float.NEGATIVE_INFINITY) "—" else String.format("%+.2f", maxV)}]",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Model Class Probabilities & Logits
                    latestResult?.let { res ->
                        Text(text = "Model Probabilities (${res.modelUsed}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val labels = when (selectedModel) {
                                MotionClassifier.MODEL_BINARY_WALKING -> listOf("WALKING", "NOT_WALKING")
                                MotionClassifier.MODEL_3_CLASS -> listOf("WALKING", "SITTING", "STANDING")
                                else -> listOf("WALKING", "UPSTAIRS", "DOWNSTAIRS", "SITTING", "STANDING", "LAYING")
                            }
                            for (idx in labels.indices) {
                                val p = res.probabilities.getOrNull(idx) ?: 0f
                                val logit = res.logits.getOrNull(idx) ?: 0f
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${labels[idx]}:", fontSize = 11.sp)
                                        Text("${(p * 100).toInt()}% (logit: ${String.format("%+.1f", logit)})", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    LinearProgressIndicator(
                                        progress = { p },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Export Diagnostics Action Button
                    Button(
                        onClick = {
                            val path = viewModel.exportDiagnosticWindow()
                            if (path != null) {
                                Toast.makeText(context, "Saved window to:\n$path", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No completed window yet. Start tracking first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Diagnostic Window to File")
                    }
                }
            }
        }
    }
}
