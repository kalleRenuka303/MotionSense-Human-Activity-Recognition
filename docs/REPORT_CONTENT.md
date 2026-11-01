# MotionSense — Technical Report Content

This document compiles the complete technical specifications, mathematical formulations, training hyperparameters, implementation code snippets, hardware/software environments, and empirical results for the **MotionSense Human Activity Recognition** project.

---

## 4. The Dataset

| Parameter | Technical Specification |
| :--- | :--- |
| **Dataset Name** | **UCI Human Activity Recognition Using Smartphones Dataset** (UCI HAR Dataset) |
| **Dataset ID** | UCI Machine Learning Repository Dataset ID **240** |
| **Source / Provenance** | Non-Linear Complex Systems Laboratory, Università degli Studi di Genova, Italy & Universitat Politècnica de Catalunya, Spain (Jorge-L. Reyes-Ortiz, Davide Anguita, Alessandro Ghio, Luca Oneto, Xavier Parra, 2012–2013) |
| **Data Modality** | **Kinematic Inertial Sensor Readings** (Triaxial Accelerometer and Triaxial Gyroscope). **Not video or images.** |
| **Sampling Frequency** | Constant **50 Hz** ($T_s = 20\text{ ms}$) |
| **Windowing Protocol** | Fixed-width sliding windows of **128 readings** ($2.56\text{ seconds}$) with **50% overlap** (64-sample / $1.28\text{ s}$ step) |
| **Total Windows (Samples)** | **10,299 windows** across 30 volunteer subjects (ages 19–48) |
| **Activity Classes (6)** | 1. `WALKING` (Dynamic forward locomotion) — 1,722 windows<br>2. `WALKING_UPSTAIRS` (Ascending stairs) — 1,544 windows<br>3. `WALKING_DOWNSTAIRS` (Descending stairs) — 1,406 windows<br>4. `SITTING` (Static posture, upright trunk, bent knees) — 1,777 windows<br>5. `STANDING` (Static posture, upright alignment) — 1,906 windows<br>6. `LAYING` (Static horizontal posture) — 1,944 windows |
| **Train / Validation / Test Split** | **Strictly Subject-Stratified (Zero Subject Leakage)**:<br>• **Train Split**: 21 subjects (70%) $\rightarrow$ 5,607 train windows + 1,745 validation windows (Total 7,352)<br>• **Test Split**: 9 unseen subjects (30%, Subjects 2, 4, 9, 10, 12, 13, 18, 20, 24) $\rightarrow$ **2,947 test windows** |
| **Input Channels (Pipeline B)** | **9 raw inertial channels**: `total_acc_x`, `total_acc_y`, `total_acc_z`, `body_acc_x`, `body_acc_y`, `body_acc_z`, `body_gyro_x`, `body_gyro_y`, `body_gyro_z` |
| **Manual Features (Pipeline A)** | **561 time- and frequency-domain variables** per window |

---

## 5. Model Architectures & Training Settings

MotionSense implements three complementary architectures across two input pipelines, plus a specialized rotation-invariant model for handheld deployment:

### 5.1 Primary Research Model: CNN-LSTM Spatio-Temporal Hybrid
- **Framework**: **PyTorch 2.x** (`torch.nn`)
- **Pipeline**: Pipeline B (Raw 9 inertial channels, shape `(batch, 128, 9)`)
- **Architecture**:
  1. `Conv1d(in_channels=9, out_channels=64, kernel_size=5, padding=2)` $\rightarrow$ `BatchNorm1d` $\rightarrow$ `ReLU()` $\rightarrow$ `Dropout(0.20)`
  2. `Conv1d(in_channels=64, out_channels=64, kernel_size=5, padding=2)` $\rightarrow$ `BatchNorm1d` $\rightarrow$ `ReLU()` $\rightarrow$ `MaxPool1d(kernel_size=2)` (Output resolution: 64 steps, 64 channels) $\rightarrow$ `Dropout(0.25)`
  3. `Transpose(batch, seq_len=64, input_size=64)`
  4. 2-Layer Stacked Recurrent LSTM (`input_size=64`, `hidden_size=128`, `dropout=0.35`, `batch_first=True`)
  5. Dense Classification Head: `Linear(128, 64)` $\rightarrow$ `ReLU()` $\rightarrow$ `Dropout(0.35)` $\rightarrow$ `Linear(64, 6)`
- **Total Parameters**: **263,814 trainable weights**
- **Training Settings**:
  - **Epochs**: 35 (Early stopping patience: 10 on validation loss; optimal checkpoint reached at epoch 16)
  - **Batch Size**: 64
  - **Optimizer**: Adam ($\beta_1 = 0.9, \beta_2 = 0.999$, $\text{weight\_decay} = 1 \times 10^{-4}$)
  - **Initial Learning Rate**: $8 \times 10^{-4}$ with `ReduceLROnPlateau(mode='min', factor=0.5, patience=4)`
  - **Loss Function**: `torch.nn.CrossEntropyLoss()`

### 5.2 Lightweight Mobile Model: 1D Convolutional Neural Network
- **Framework**: **PyTorch 2.x** $\rightarrow$ Exported to **ONNX Runtime Mobile** (`opset_version=14`)
- **Pipeline**: Pipeline B (Raw 9 inertial channels, shape `(batch, 128, 9)`)
- **Architecture**:
  1. `Conv1d(9, 64, kernel_size=5, padding=2)` $\rightarrow$ `BatchNorm1d` $\rightarrow$ `ReLU()` $\rightarrow$ `MaxPool1d(2)` $\rightarrow$ `Dropout(0.20)`
  2. `Conv1d(64, 128, kernel_size=5, padding=2)` $\rightarrow$ `BatchNorm1d` $\rightarrow$ `ReLU()` $\rightarrow$ `MaxPool1d(2)` $\rightarrow$ `Dropout(0.25)`
  3. `Conv1d(128, 128, kernel_size=3, padding=1)` $\rightarrow$ `BatchNorm1d` $\rightarrow$ `ReLU()` $\rightarrow$ `AdaptiveAvgPool1d(1)` $\rightarrow$ `Dropout(0.30)`
  4. `Linear(128, 64)` $\rightarrow$ `ReLU()` $\rightarrow$ `Dropout(0.30)` $\rightarrow$ `Linear(64, 6)`
- **Total Parameters**: **111,238 trainable weights**
- **Training Settings**:
  - **Epochs**: 30 (Optimal checkpoint at epoch 13)
  - **Batch Size**: 64
  - **Optimizer**: Adam ($\text{lr} = 5 \times 10^{-4}$, $\text{weight\_decay} = 1 \times 10^{-4}$)
  - **Loss Function**: `torch.nn.CrossEntropyLoss()`

### 5.3 Baseline Model: Random Forest Classifier
- **Framework**: **Scikit-Learn**
- **Pipeline**: Pipeline A (561 statistical features)
- **Settings**: 150 Estimators, Max Depth = 25, Min Samples Split = 4, Min Samples Leaf = 2, Gini Impurity.

### 5.4 Rotation-Invariant Handheld Model: Invariant Binary Classifier
- **Target**: Handheld smartphones held vertically in hand
- **Invariance Technique**: Internal 3D Euclidean magnitude computation:
  $$\|a_{\text{total}}\| = \sqrt{a_x^2 + a_y^2 + a_z^2}, \quad \|a_{\text{body}}\| = \sqrt{a_{\text{body},x}^2 + a_{\text{body},y}^2 + a_{\text{body},z}^2}, \quad \|\omega\| = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$$
- **Parameters**: 642 weights (11.0 KB ONNX file)

---

## 6. Implementation Code Snippets

### 6.1 PyTorch CNN-LSTM Model Implementation
```python
import torch
import torch.nn as nn

class HAR_CNNLSTM(nn.Module):
    def __init__(self, in_channels: int = 9, num_classes: int = 6):
        super().__init__()
        # 1D Convolutional Spatial Feature Extraction
        self.conv1 = nn.Sequential(
            nn.Conv1d(in_channels, 64, kernel_size=5, padding=2),
            nn.BatchNorm1d(64),
            nn.ReLU(),
            nn.Dropout(0.20)
        )
        self.conv2 = nn.Sequential(
            nn.Conv1d(64, 64, kernel_size=5, padding=2),
            nn.BatchNorm1d(64),
            nn.ReLU(),
            nn.MaxPool1d(kernel_size=2),  # Timesteps: 128 -> 64
            nn.Dropout(0.25)
        )
        
        # 2-Layer Stacked Bidirectional/Unidirectional LSTM
        self.lstm = nn.LSTM(
            input_size=64,
            hidden_size=128,
            num_layers=2,
            batch_first=True,
            dropout=0.35
        )
        
        # Dense Classification Head
        self.classifier = nn.Sequential(
            nn.Linear(128, 64),
            nn.ReLU(),
            nn.Dropout(0.35),
            nn.Linear(64, num_classes)
        )
        
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # Input shape: (batch_size, 128, 9) -> transpose to (batch_size, 9, 128)
        x = x.transpose(1, 2)
        feat = self.conv2(self.conv1(x))
        # Transpose for LSTM: (batch_size, 64_channels, 64_steps) -> (batch_size, 64_steps, 64_channels)
        feat = feat.transpose(1, 2)
        lstm_out, _ = self.lstm(feat)
        # Take the final temporal representation
        out = self.classifier(lstm_out[:, -1, :])
        return out
```

### 6.2 3rd-Order Digital Butterworth Filter (Kotlin / Direct Form II Transposed)
```kotlin
package com.motionsense.har.sensor

class TriaxialButterworthFilter {
    // 3rd-order Butterworth low-pass: fc = 0.3 Hz, fs = 50 Hz
    // b = [7.02055627e-05, 2.10616688e-04, 2.10616688e-04, 7.02055627e-05]
    // a = [1.0, -2.92544770, 2.85387400, -0.92837466]
    private val b = doubleArrayOf(7.02055627e-05, 2.10616688e-04, 2.10616688e-04, 7.02055627e-05)
    private val a = doubleArrayOf(1.0, -2.92544770, 2.85387400, -0.92837466)

    // Direct Form II Transposed state registers (3 state variables per axis)
    private val wX = DoubleArray(3)
    private val wY = DoubleArray(3)
    private val wZ = DoubleArray(3)
    private var initialized = false

    fun filterGravity(totalAcc: FloatArray): FloatArray {
        if (!initialized) {
            initDc(totalAcc[0].toDouble(), totalAcc[1].toDouble(), totalAcc[2].toDouble())
            initialized = true
        }
        val gx = step(totalAcc[0].toDouble(), wX)
        val gy = step(totalAcc[1].toDouble(), wY)
        val gz = step(totalAcc[2].toDouble(), wZ)
        return floatArrayOf(gx.toFloat(), gy.toFloat(), gz.toFloat())
    }

    private fun step(x: Double, w: DoubleArray): Double {
        val y = b[0] * x + w[0]
        w[0] = b[1] * x - a[1] * y + w[1]
        w[1] = b[2] * x - a[2] * y + w[2]
        w[2] = b[3] * x - a[3] * y
        return y
    }

    private fun initDc(dcX: Double, dcY: Double, dcZ: Double) {
        initAxisDc(dcX, wX)
        initAxisDc(dcY, wY)
        initAxisDc(dcZ, wZ)
    }

    private fun initAxisDc(dc: Double, w: DoubleArray) {
        // Compute steady-state state conditions for DC input to eliminate startup transient
        w[0] = (b[1] - a[1] * b[0]) * dc + (b[2] - a[2] * b[0]) * dc + (b[3] - a[3] * b[0]) * dc
        w[1] = (b[2] - a[2] * b[0]) * dc + (b[3] - a[3] * b[0]) * dc
        w[2] = (b[3] - a[3] * b[0]) * dc
    }
}
```

### 6.3 Android On-Device ONNX Runtime Inference Runner
```kotlin
package com.motionsense.har.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class MotionClassifier(private val context: android.content.Context) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("models/walking_binary.onnx").readBytes()
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    fun classifyWindow(windowData: FloatArray): Pair<Int, Float> {
        // windowData is flattened [128 * 9 = 1152] floats
        val tensorShape = longArrayOf(1, 128, 9)
        val floatBuffer = FloatBuffer.wrap(windowData)
        val inputTensor = OnnxTensor.createTensor(env, floatBuffer, tensorShape)

        val results = session.run(mapOf("input" to inputTensor))
        val outputTensor = results[0].value as Array<FloatArray>
        val logits = outputTensor[0]

        // Softmax normalization
        val probs = softmax(logits)
        var maxIdx = 0
        var maxProb = 0f
        for (i in probs.indices) {
            if (probs[i] > maxProb) {
                maxProb = probs[i]
                maxIdx = i
            }
        }
        inputTensor.close()
        results.close()
        return Pair(maxIdx, maxProb)
    }
}
```

---

## 7. Hardware & Software Environments

### 7.1 Development & Training Environment
- **Operating System**: macOS 15.3 (Darwin arm64) / Linux Ubuntu 22.04 LTS
- **Processor / Hardware**: Apple M-Series / Google Colab T4 GPU (CUDA 12.1)
- **Python Version**: Python 3.9.6 / 3.10.12
- **Core Python Libraries**:
  - `torch==2.2.0` (Deep learning training & PyTorch models)
  - `onnx==1.15.0` & `onnxruntime==1.17.1` (ONNX export & mobile parity evaluation)
  - `scikit-learn==1.4.0` (Random Forest, cross-validation & evaluation metrics)
  - `scipy==1.12.0` (Digital filter design, `signal.butter`)
  - `numpy==1.26.4` (Array manipulation & numerical checks)
  - `fastapi==0.110.0` & `uvicorn==0.28.0` (Research REST API)

### 7.2 Mobile Deployment Environment
- **Device Hardware**: Physical Android Smartphones (Samsung, Google Pixel, Xiaomi) with embedded STMicroelectronics / Bosch BMI triaxial accelerometer and gyroscope
- **Android OS**: Android 8.0 Oreo (API 26) through Android 16 (API 36)
- **Java / JDK**: Adoptium Temurin OpenJDK 17.0.21
- **Android Gradle Plugin**: AGP 8.9.0 / Gradle 9.1
- **Mobile Runtime Libraries**:
  - `ai.onnxruntime:onnxruntime-android:1.17.0`
  - `androidx.compose.material3:material3:1.2.1`
  - `androidx.room:room-runtime:2.6.1`

---

## 8. Quantitative Evaluation Results

Evaluated strictly on the **official UCI HAR Test Set ($N = 2,947$ windows)** from the 9 unseen volunteers (zero subject leakage):

### 8.1 Overall Comparative Summary Table

| Model Architecture | Input Channels / Dimension | Validation Accuracy | Official Test Accuracy | Macro Precision | Macro Recall | Macro F1-Score | Weighted F1-Score | Model File Size |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Rotation-Invariant Binary** | $[1, 128, 9]$ (Euclidean Norms) | 99.88% | **99.86%** | **99.86%** | **99.87%** | **99.86%** | **99.86%** | **11.0 KB** |
| **1D CNN Baseline** | $[1, 128, 9]$ (Raw Inertial) | 90.54% | **93.99%** | **94.04%** | **94.04%** | **93.99%** | **93.96%** | **434 KB** |
| **Random Forest Baseline** | 561 Features (Pipeline A) | 90.43% | **91.86%** | 91.95% | 91.52% | **91.64%** | **91.83%** | 5.7 MB |
| **CNN-LSTM Spatio-Temporal** | $[1, 128, 9]$ (Raw Inertial) | **92.26%** | **90.74%** | 91.10% | 90.77% | **90.77%** | **90.71%** | 1,034 KB |
| **3-Class Classifier** | $[1, 128, 9]$ (Raw Inertial) | 88.10% | **87.43%** | 86.02% | 79.53% | **78.47%** | **86.12%** | **11.6 KB** |

---

### 8.2 Per-Class Classification Report: 1D CNN (Pipeline B, Highest 6-Class Accuracy)

| Activity Class | Precision | Recall | F1-Score | Support (Windows) |
| :--- | :---: | :---: | :---: | :---: |
| **`WALKING`** | **0.9688** | **1.0000** | **0.9841** | 496 |
| **`WALKING_UPSTAIRS`** | **0.9931** | **0.9151** | **0.9525** | 471 |
| **`WALKING_DOWNSTAIRS`** | **0.9302** | **0.9833** | **0.9560** | 420 |
| **`SITTING`** | **0.8816** | **0.8493** | **0.8652** | 491 |
| **`STANDING`** | **0.8782** | **0.8947** | **0.8864** | 532 |
| **`LAYING`** | **0.9908** | **1.0000** | **0.9954** | 537 |
| **Macro Average** | **0.9404** | **0.9404** | **0.9399** | **2,947** |
| **Weighted Average** | **0.9407** | **0.9399** | **0.9396** | **2,947** |

---

### 8.3 Per-Class Classification Report: CNN-LSTM Spatio-Temporal Model

| Activity Class | Precision | Recall | F1-Score | Support (Windows) |
| :--- | :---: | :---: | :---: | :---: |
| **`WALKING`** | 0.8792 | 0.9980 | 0.9348 | 496 |
| **`WALKING_UPSTAIRS`** | 0.9854 | 0.8577 | 0.9171 | 471 |
| **`WALKING_DOWNSTAIRS`** | 0.9474 | 0.9429 | 0.9451 | 420 |
| **`SITTING`** | 0.8504 | 0.8452 | 0.8478 | 491 |
| **`STANDING`** | 0.8647 | 0.8722 | 0.8684 | 532 |
| **`LAYING`** | 0.9388 | 0.9702 | 0.9542 | 537 |
| **Macro Average** | **0.9110** | **0.9077** | **0.9077** | **2,947** |
| **Weighted Average** | **0.9103** | **0.9074** | **0.9071** | **2,947** |

---

### 8.4 Per-Class Classification Report: Rotation-Invariant Binary Model (`walking_binary.onnx`)

| Activity Class | Precision | Recall | F1-Score | Support (Windows) |
| :--- | :---: | :---: | :---: | :---: |
| **`WALKING`** | **0.9971** | **1.0000** | **0.9986** | 1,387 |
| **`NOT_WALKING` (Stationary)** | **1.0000** | **0.9974** | **0.9987** | 1,560 |
| **Overall Accuracy** | — | — | **0.9986** | **2,947** |

---

## 9. Confusion Matrices

### 9.1 Confusion Matrix: 1D CNN ($N = 2,947$)
```
True \ Pred      WALK   UPSTAIRS   DOWNSTAIRS   SIT   STAND   LAY
WALKING          496        0           0        0       0     0
UPSTAIRS           9      431          31        0       0     0
DOWNSTAIRS         7        0         413        0       0     0
SITTING            0        3           0      417      66     5
STANDING           0        0           0       56     476     0
LAYING             0        0           0        0       0   537
```

### 9.2 Confusion Matrix: CNN-LSTM ($N = 2,947$)
```
True \ Pred      WALK   UPSTAIRS   DOWNSTAIRS   SIT   STAND   LAY
WALKING          495        1           0        0       0     0
UPSTAIRS          45      404          22        0       0     0
DOWNSTAIRS        23        1         396        0       0     0
SITTING            0        4           0      415      72     0
STANDING           0        0           0       68     464     0
LAYING             0        0           0        5      11   521
```

### 9.3 Confusion Matrix: Rotation-Invariant Binary Model ($N = 2,947$)
```
True \ Pred            WALKING        STATIONARY (NOT_WALKING)
WALKING                 1,387                     0
STATIONARY                  4                 1,556
```

---

## 10. Prediction Speed, Inference Latency & Efficiency

Benchmarked across desktop CPU, GPU, and physical mobile processors:

| Model | Platform / Runtime | Device Hardware | Latency per 128x9 Window | Throughput (Windows / sec) | Mobile RAM / Footprint |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Rotation-Invariant Binary** | ONNX Runtime Mobile | Qualcomm Snapdragon 8 Gen 2 | **0.32 ms** | > 3,000 | 11.0 KB |
| **1D CNN** | ONNX Runtime Mobile | Qualcomm Snapdragon 8 Gen 2 | **1.14 ms** | ~877 | 434 KB |
| **CNN-LSTM** | ONNX Runtime Mobile | Qualcomm Snapdragon 8 Gen 2 | **4.68 ms** | ~213 | 1,034 KB |
| **1D CNN** | PyTorch / Python CPU | Apple M-Series CPU | **0.41 ms** | ~2,430 | 1.3 MB (.pt) |
| **CNN-LSTM** | PyTorch / Python CPU | Apple M-Series CPU | **0.86 ms** | ~1,160 | 3.0 MB (.pt) |
| **1D CNN** | PyTorch CUDA | NVIDIA T4 GPU | **0.18 ms** | ~5,500 | — |

> [!TIP]
> At a 50 Hz sampling rate, each 128-sample window represents **2.56 seconds** of real time, with consecutive windows produced every **1.28 seconds** (64-sample stride). An inference latency of **1.14 ms** uses less than **0.1%** of available mobile CPU time, ensuring negligible battery drain and zero thermal throttling during 24-hour foreground monitoring.
