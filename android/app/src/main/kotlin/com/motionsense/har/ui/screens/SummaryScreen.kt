package com.motionsense.har.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionsense.har.ui.theme.*
import com.motionsense.har.ui.viewmodel.MotionViewModel

@Composable
fun SummaryScreen(viewModel: MotionViewModel) {
    val summary by viewModel.dailySummary.collectAsState()
    val totalSec = summary.totalActiveSec + summary.totalSedentarySec

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Daily Activity Summary",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aggregated posture and movement breakdown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active vs Sedentary Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ActivityWalking.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = ActivityWalking, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Active Time",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActivityWalking
                        )
                    }
                    val activeMins = summary.totalActiveSec / 60
                    Text(
                        text = "$activeMins mins",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActivityWalking
                    )
                    Text(
                        text = "Walking & Stairs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ActivitySitting.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AirlineSeatReclineNormal, contentDescription = null, tint = ActivitySitting, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Sedentary",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActivitySitting
                        )
                    }
                    val sedentaryMins = summary.totalSedentarySec / 60
                    Text(
                        text = "$sedentaryMins mins",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActivitySitting
                    )
                    Text(
                        text = "Sitting, Standing, Laying",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Detailed Activity Duration Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Time Spent Per Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ActivityBarRow(
                    label = "Walking",
                    seconds = summary.walkingSec,
                    totalSeconds = totalSec,
                    color = ActivityWalking,
                    icon = Icons.Default.DirectionsWalk
                )

                ActivityBarRow(
                    label = "Stairs (Up & Down)",
                    seconds = summary.stairsSec,
                    totalSeconds = totalSec,
                    color = ActivityWalkingUpstairs,
                    icon = Icons.Default.NordicWalking
                )

                ActivityBarRow(
                    label = "Sitting",
                    seconds = summary.sittingSec,
                    totalSeconds = totalSec,
                    color = ActivitySitting,
                    icon = Icons.Default.AirlineSeatReclineNormal
                )

                ActivityBarRow(
                    label = "Standing",
                    seconds = summary.standingSec,
                    totalSeconds = totalSec,
                    color = ActivityStanding,
                    icon = Icons.Default.AccessibilityNew
                )

                ActivityBarRow(
                    label = "Laying",
                    seconds = summary.layingSec,
                    totalSeconds = totalSec,
                    color = ActivityLaying,
                    icon = Icons.Default.Hotel
                )
            }
        }
    }
}

@Composable
fun ActivityBarRow(
    label: String,
    seconds: Long,
    totalSeconds: Long,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val fraction = if (totalSeconds > 0) (seconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (fraction * 100).toInt()
    val mins = seconds / 60

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$mins m ($percent%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
