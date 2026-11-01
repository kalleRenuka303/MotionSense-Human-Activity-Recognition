package com.motionsense.har.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionsense.har.data.model.ActivityType
import com.motionsense.har.ml.MotionClassifier
import com.motionsense.har.ui.theme.*
import com.motionsense.har.ui.viewmodel.MotionViewModel

@Composable
fun HomeScreen(viewModel: MotionViewModel) {
    val currentActivity by viewModel.currentActivity.collectAsState()
    val confidence by viewModel.confidence.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val durationSec by viewModel.trackingDurationSec.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val sensorAvailability by viewModel.sensorAvailability.collectAsState()

    val activityColor by animateColorAsState(
        targetValue = when (currentActivity) {
            ActivityType.WALKING -> ActivityWalking
            ActivityType.WALKING_UPSTAIRS -> ActivityWalkingUpstairs
            ActivityType.WALKING_DOWNSTAIRS -> ActivityWalkingDownstairs
            ActivityType.SITTING -> ActivitySitting
            ActivityType.STANDING -> ActivityStanding
            ActivityType.LAYING -> ActivityLaying
            ActivityType.UNCERTAIN -> ActivityUncertain
        }, label = "activityColor"
    )

    val activityIcon: ImageVector = when (currentActivity) {
        ActivityType.WALKING -> Icons.Default.DirectionsWalk
        ActivityType.WALKING_UPSTAIRS -> Icons.Default.NordicWalking
        ActivityType.WALKING_DOWNSTAIRS -> Icons.Default.DirectionsRun
        ActivityType.SITTING -> Icons.Default.AirlineSeatReclineNormal
        ActivityType.STANDING -> Icons.Default.AccessibilityNew
        ActivityType.LAYING -> Icons.Default.Hotel
        ActivityType.UNCERTAIN -> Icons.Default.HelpOutline
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MotionSense",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Continuous On-Device HAR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Live status badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isTracking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isTracking) Color(0xFF10B981) else Color(0xFF94A3B8))
                    )
                    Text(
                        text = if (isTracking) "TRACKING" else "IDLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Recognized Activity Main Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(activityColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activityIcon,
                        contentDescription = currentActivity.displayName,
                        tint = activityColor,
                        modifier = Modifier.size(54.dp)
                    )
                }

                // Current Activity Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentActivity.displayName.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = activityColor
                    )
                    Text(
                        text = currentActivity.category + " Motion Profile",
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

                // Tracking Duration
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
                            text = "Buffer Windows",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${telemetry.samplesCollected / 64} processed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tracking Controls (Start / Stop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isTracking) {
                Button(
                    onClick = { viewModel.startTracking() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActivityWalking
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START TRACKING",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
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
                Text(
                    text = "Inference Neural Network",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedModel == MotionClassifier.MODEL_CNN_LSTM,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_CNN_LSTM) },
                        label = { Text("CNN-LSTM (Research)") },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_CNN_LSTM) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedModel == MotionClassifier.MODEL_1D_CNN,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_1D_CNN) },
                        label = { Text("1D CNN (Fast)") },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_1D_CNN) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Sensor Telemetry Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Sensor Signals (50 Hz)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Buffer: ${telemetry.bufferFillPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Accelerometer readings (g)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total Acceleration (g = 9.81 m/s²):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryAxis(label = "X", value = telemetry.totalAccX)
                        TelemetryAxis(label = "Y", value = telemetry.totalAccY)
                        TelemetryAxis(label = "Z", value = telemetry.totalAccZ)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Gyroscope readings (rad/s)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Body Angular Velocity (rad/s):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryAxis(label = "X", value = telemetry.gyroX)
                        TelemetryAxis(label = "Y", value = telemetry.gyroY)
                        TelemetryAxis(label = "Z", value = telemetry.gyroZ)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryAxis(label: String, value: Float) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.width(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = String.format("%+.2f", value),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
