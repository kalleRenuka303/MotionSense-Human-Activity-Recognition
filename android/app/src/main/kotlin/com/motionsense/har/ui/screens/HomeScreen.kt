package com.motionsense.har.ui.screens

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

    var showDiagnostics by remember { mutableStateOf(false) }

    val activityColor by animateColorAsState(
        targetValue = when (currentActivity) {
            ActivityType.WALKING -> ActivityWalking
            ActivityType.WALKING_UPSTAIRS -> ActivityWalkingUpstairs
            ActivityType.WALKING_DOWNSTAIRS -> ActivityWalkingDownstairs
            ActivityType.SITTING -> ActivitySitting
            ActivityType.STANDING -> ActivityStanding
            ActivityType.LAYING -> ActivityLaying
            ActivityType.UNCERTAIN -> ActivityUncertain
        },
        label = "activityColor"
    )

    val activityIcon: ImageVector = when (currentActivity) {
        ActivityType.WALKING -> Icons.Default.DirectionsWalk
        ActivityType.WALKING_UPSTAIRS -> Icons.Default.DirectionsRun
        ActivityType.WALKING_DOWNSTAIRS -> Icons.Default.DirectionsWalk
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
                        text = "CRITICAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = ActivityWalking,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "UCI HAR was recorded with a horizontal waist phone. Select your placement to align sensor axes to the model reference frame:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = placement == PhonePlacement.FRONT_POCKET_PORTRAIT,
                        onClick = { viewModel.setPlacement(PhonePlacement.FRONT_POCKET_PORTRAIT) },
                        label = { Text("Front Pocket", fontSize = 12.sp) },
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

                    FilterChip(
                        selected = placement == PhonePlacement.HANDHELD_PORTRAIT,
                        onClick = { viewModel.setPlacement(PhonePlacement.HANDHELD_PORTRAIT) },
                        label = { Text("Handheld", fontSize = 12.sp) },
                        leadingIcon = if (placement == PhonePlacement.HANDHELD_PORTRAIT) {
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
                        selected = selectedModel == MotionClassifier.MODEL_1D_CNN,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_1D_CNN) },
                        label = { Text("1D CNN (93.99% • Fast)") },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_1D_CNN) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedModel == MotionClassifier.MODEL_CNN_LSTM,
                        onClick = { viewModel.setModel(MotionClassifier.MODEL_CNN_LSTM) },
                        label = { Text("CNN-LSTM (90.74%)") },
                        leadingIcon = if (selectedModel == MotionClassifier.MODEL_CNN_LSTM) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Toggle Sensor Diagnostics Mode (Phase 4 requirement)
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
            Text(if (showDiagnostics) "Hide Sensor Diagnostics" else "Show Sensor Diagnostics (Phase 4)")
        }

        // PHASE 4: Complete Sensor Diagnostics Panel
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
                            val labels = listOf("WALKING", "UPSTAIRS", "DOWNSTAIRS", "SITTING", "STANDING", "LAYING")
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
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
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
