package com.motionsense.har.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionsense.har.ui.theme.ActivityWalking

@Composable
fun ModelInfoScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Model Architecture",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real-time edge neural networks for smartphone activity recognition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Binary Walking Classifier (Primary Recommended Model)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Binary Invariant Walking Classifier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActivityWalking
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ActivityWalking.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActivityWalking,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Rotation-invariant 2-class classifier (Walking vs Stationary) utilizing 3D Euclidean magnitude norms (||v||). Mathematically immune to phone tilt and vertical hand orientation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                ModelSpecRow("Input Tensor", "[1, 128, 9] (Total Acc, Body Acc, Gyro)")
                ModelSpecRow("Test Accuracy", "99.86% (Subject-independent)")
                ModelSpecRow("Walking Recall", "100.00% (1,387 / 1,387 windows)")
                ModelSpecRow("Walking Precision", "99.71%")
                ModelSpecRow("False Positive Rate", "0.26% (Static flagged as walking)")
                ModelSpecRow("Mobile Size", "11.0 KB (Ultra-compact ONNX)")
                ModelSpecRow("Inference Latency", "< 0.5 ms on phone CPU")
            }
        }

        // 3-Class Classifier (Experimental)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3-Class Classifier (Experimental)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "EXPERIMENTAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Distinguishes Walking vs Sitting vs Standing. NOTE: In a handheld position, gravity vector and hand motion are virtually identical between sitting and standing, leading to expected static confusion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                ModelSpecRow("Input Tensor", "[1, 128, 9]")
                ModelSpecRow("Test Accuracy", "87.43% (UCI HAR Test set)")
                ModelSpecRow("Walking Recall", "100.00%")
                ModelSpecRow("Sitting Recall", "42.36% (Confusion with standing)")
                ModelSpecRow("Standing Recall", "96.24%")
                ModelSpecRow("Mobile Size", "11.6 KB (ONNX Runtime)")
            }
        }

        // 6-Class 1D CNN Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6-Class 1D CNN (UCI HAR Standard)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "6-CLASS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Multi-stage 1D CNN with Batch Normalization and Global Average Pooling trained on UCI HAR horizontal waist sensor orientation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                ModelSpecRow("Input Tensor", "[1, 128, 9]")
                ModelSpecRow("Parameters", "111,238 weights")
                ModelSpecRow("Test Set Acc", "93.99% (Official test set)")
                ModelSpecRow("Macro F1", "93.99%")
                ModelSpecRow("Mobile Size", "434 KB (ONNX Runtime)")
            }
        }

        // UCI HAR Dataset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dataset & Preprocessing Protocol",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "• UCI HAR Dataset (30 subjects, 10,299 windows)\n" +
                            "• Zero subject leakage (21 train, 9 test)\n" +
                            "• 9 Inertial Channels: Total Acc (X, Y, Z), Body Acc (X, Y, Z), Gyroscope (X, Y, Z)\n" +
                            "• Normalization: Acceleration divided by standard gravity 9.80665 m/s² to match 'g' units\n" +
                            "• Sampling: 50 Hz (20 ms timestep), 128-sample window (2.56s) with 50% overlap (1.28s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModelSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
