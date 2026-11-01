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
                text = "Deep learning models trained on UCI HAR Dataset 240",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CNN-LSTM Card (Primary Proposed Model)
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
                        text = "CNN-LSTM (Proposed Research)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActivityWalking
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ActivityWalking.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "PRIMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActivityWalking,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Combines spatial 1D convolutional layers for local movement pattern extraction with recurrent LSTM cells to model temporal transition dynamics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                ModelSpecRow("Input Tensor", "[1, 128, 9] (128 steps @ 50 Hz, 9 channels)")
                ModelSpecRow("Parameters", "263,814 trainable weights")
                ModelSpecRow("Validation Acc", "92.26% (Subject-stratified)")
                ModelSpecRow("Test Set Acc", "90.74% (Official test set)")
                ModelSpecRow("Test Macro F1", "90.77%")
                ModelSpecRow("Mobile Size", "1,034 KB (ONNX Runtime)")
            }
        }

        // 1D CNN Card
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
                        text = "1D Convolutional Neural Network",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "FAST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Multi-stage 1D CNN with Batch Normalization and Global Average Pooling for ultra-low latency mobile inference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                ModelSpecRow("Input Tensor", "[1, 128, 9] (128 steps @ 50 Hz, 9 channels)")
                ModelSpecRow("Parameters", "111,238 weights")
                ModelSpecRow("Test Set Acc", "93.99% (Official test set)")
                ModelSpecRow("Test Macro F1", "93.99%")
                ModelSpecRow("Inference Latency", "< 5 ms on phone CPU")
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
                    text = "• UCI HAR Dataset 240 (30 subjects, 10,299 windows)\n" +
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
