# MotionSense — Native Android Application Documentation

## 1. Executive Summary

**MotionSense** is a native Android mobile application for real-time Human Activity Recognition (HAR) using embedded smartphone sensors (accelerometer and gyroscope). Unlike web applications or PWA wrappers, MotionSense is a genuine, high-performance native application engineered with **Kotlin**, **Jetpack Compose (Material 3)**, **Android SensorManager APIs**, **ONNX Runtime Android**, and a persistent **Android Foreground Service** that continuously classifies physical movement even when the screen is turned off or when navigating other apps.

The application natively detects and classifies the 6 standardized physical activities:
1. **Walking** (Dynamic)
2. **Walking Upstairs** (Dynamic)
3. **Walking Downstairs** (Dynamic)
4. **Sitting** (Static)
5. **Standing** (Static)
6. **Laying** (Static)

---

## 2. System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI (Material 3)                      │
│   HomeScreen  │  HistoryScreen  │  SummaryScreen  │  ModelInfoScreen   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (StateFlow / Coroutines)
┌───────────────────────────────────▼────────────────────────────────────┐
│                  MotionViewModel (AndroidViewModel)                    │
└──────────────┬──────────────────────────────────────────┬──────────────┘
               │ (Bound Service Connection)               │ (Flows)
┌──────────────▼──────────────────────────┐    ┌──────────▼──────────────┐
│  MotionTrackingService (Foreground)     │    │   MotionSenseDatabase   │
│  - Ongoing Status Notification          │    │   - SQLiteOpenHelper    │
│  - CPU WakeLock (Continuous Tracking)   │    │   - ActivityRecord Table│
└──────────────┬──────────────────────────┘    └─────────────────────────┘
               │
      ┌────────┴────────┐
      ▼                 ▼
┌──────────────┐ ┌───────────────────────────────────────────────────────┐
│SensorCollector│ │ MotionClassifier (ONNX Runtime)                       │
│- Accel (50Hz)│ │ - 1D CNN (93.99% UCI HAR Test Accuracy)               │
│- Gyro (50Hz) │ │ - CNN-LSTM (90.74% UCI HAR Test Accuracy)             │
│- Ring Buffer │ │ - Input: [1, 9, 128] float32 tensor                   │
│- Normalizer  │ │ - Softmax & Activity Mapping                          │
└──────────────┘ └───────────────────────────────────────────────────────┘
```

---

## 3. Sensor Acquisition & Signal Processing Pipeline

### 3.1 Sampling Rate & Normalization
The application implements continuous multi-sensor acquisition via `android.hardware.SensorManager`:
- **Accelerometer (`Sensor.TYPE_ACCELEROMETER`)**: Measures acceleration in $\text{m/s}^2$ including gravity. To maintain exact input parity with the UCI HAR research dataset (which standardizes signals in units of $g$), raw acceleration values are scaled:
  $$a_g = \frac{a_{\text{m/s}^2}}{9.80665}$$
  Body acceleration is extracted in real-time using a single-pole IIR high-pass filter ($RC = 0.5$ s, $\alpha \approx 0.8$):
  $$g_t = \alpha \cdot g_{t-1} + (1 - \alpha) \cdot a_t, \quad a_{\text{body}, t} = a_t - g_t$$
- **Gyroscope (`Sensor.TYPE_GYROSCOPE`)**: Measures angular velocity natively in $\text{rad/s}$, which identically matches the angular velocity units of the UCI HAR dataset.

### 3.2 Sliding Window Buffer
- **Sampling Frequency**: $50\text{ Hz}$ ($20\text{ ms}$ interval, registered with `SENSOR_DELAY_GAME` / `SENSOR_DELAY_FASTEST`).
- **Window Length**: $128\text{ samples}$ ($2.56\text{ seconds}$).
- **Overlap**: $50\%$ ($64\text{ samples} = 1.28\text{ seconds}$ stride).
- **Channels**: 9 synchronized sensor channels formatted as a float array of shape `[1, 9, 128]`:
  1. `total_acc_x` ($g$)
  2. `total_acc_y` ($g$)
  3. `total_acc_z` ($g$)
  4. `body_acc_x` ($g$)
  5. `body_acc_y` ($g$)
  6. `body_acc_z` ($g$)
  7. `body_gyro_x` ($\text{rad/s}$)
  8. `body_gyro_y` ($\text{rad/s}$)
  9. `body_gyro_z` ($\text{rad/s}$)

---

## 4. Mobile Deep Learning Inference

### 4.1 On-Device ONNX Engine
Models are embedded directly inside the Android application assets (`app/src/main/assets/models/`) and executed using `com.microsoft.onnxruntime:onnxruntime-android:1.17.0`.

### 4.2 Embedded Models & Parity Verification
Both pre-trained PyTorch deep learning models were exported to ONNX format with strict numerical verification against the original PyTorch weights:

| Model Architecture | Asset Name | File Size | Strict Subject-Independent Test Acc | Max Abs Diff (PyTorch vs ONNX) |
|---|---|---|---|---|
| **1D CNN** (Baseline) | `cnn_1d.onnx` | 434 KB | **93.99%** | $3.81 \times 10^{-6}$ (< 1e-4) |
| **CNN-LSTM** (Primary) | `cnn_lstm.onnx` | 1,034 KB | **90.74%** | $3.22 \times 10^{-6}$ (< 1e-4) |

*Note on Accuracies: In literature, randomly shuffled splits (with subject leakage across train and test) frequently report inflated accuracies of ~95%–96%. In contrast, MotionSense strictly enforces the official subject-independent benchmark protocol (unseen test subjects 2, 4, 9, 10, 12, 13, 18, 20, 24), yielding genuine, non-overfitted test accuracies of 93.99% (1D CNN) and 90.74% (CNN-LSTM).*

*The maximum numerical difference between the PyTorch float32 model outputs and the on-device ONNX Runtime outputs across randomized and real test tensors is $< 4 \times 10^{-6}$, guaranteeing zero loss of classification fidelity.*

---

## 5. Background Tracking & Battery Management

### 5.1 Android Foreground Service
Continuous activity tracking is managed by `MotionTrackingService`:
- Registered with foreground service types: `health` and `dataSync`.
- Displays a persistent, non-dismissible notification (`MotionSense Active`) showing the current recognized activity and live confidence score in real time.
- Holds a `PowerManager.PARTIAL_WAKE_LOCK` to maintain sensor polling and model execution when the device is locked in a pocket.
- Stops cleanly and releases all sensor listeners and wake locks when the user taps **Stop Tracking** in the UI.

### 5.2 Required Permissions
Configured in `AndroidManifest.xml`:
- `android.permission.ACTIVITY_RECOGNITION` (Android 10+)
- `android.permission.FOREGROUND_SERVICE` (Android 9+)
- `android.permission.FOREGROUND_SERVICE_HEALTH` (Android 14+)
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC` (Android 14+)
- `android.permission.POST_NOTIFICATIONS` (Android 13+)
- `android.permission.WAKE_LOCK`
- `android.permission.HIGH_SAMPLING_RATE_SENSORS` (Android 12+ for 50 Hz polling)

---

## 6. Local SQLite Storage

The app stores every classified activity segment in a lightweight SQLite database (`MotionSenseDatabase`):
- **Schema**:
  - `id`: INTEGER PRIMARY KEY AUTOINCREMENT
  - `timestamp`: INTEGER (epoch millis)
  - `activity_name`: TEXT
  - `confidence`: REAL
  - `duration_seconds`: INTEGER
  - `model_used`: TEXT
  - `date_string`: TEXT (YYYY-MM-DD)
- Delivered to Compose UI via Kotlin `Flow<List<ActivityRecord>>` with automatic updates on new predictions.

---

## 7. User Interface (Jetpack Compose & Material 3)

The native user interface provides 5 distinct screens accessible via a bottom navigation bar:

1. **Live Activity (Home)**:
   - High-contrast activity gauge with dynamic icons and category indicators (`Dynamic` vs `Static`).
   - Confidence percentage meter with color-coded safety thresholds (>80% Green, 60-80% Blue, <60% Amber).
   - Real-time 50 Hz sensor telemetry readout for 3-axis Accelerometer and 3-axis Gyroscope.
   - Model switcher (toggle between **CNN-LSTM** and **1D CNN** with immediate on-device switching).
   - One-tap **Start Tracking / Stop Tracking** FAB/Button.
2. **Activity History**:
   - Scrollable timeline of detected activity intervals with duration badges and timestamps.
   - Clear History action with confirmation dialog.
3. **Daily Activity Summary**:
   - High-level metric cards: Total Active Time (Walking + Stairs) vs Total Sedentary Time (Sitting + Standing + Laying).
   - Proportional duration progress bars for each of the 6 activities.
4. **Model Information**:
   - Technical specifications for both CNN-LSTM and 1D CNN models.
   - Parameter counts, receptive fields, and UCI HAR benchmark performance metrics.
5. **Settings & Diagnostics**:
   - Real-time sensor hardware detection (vendor, power, resolution for Accelerometer and Gyroscope).
   - Interactive permission check buttons (`Activity Recognition`, `Notifications`).
   - Battery optimization whitelist instructions for uninterrupted long-term logging.

---

## 8. Artifacts & Installation

### 8.1 Deliverable Files
- **Debug APK**: `artifacts/apk/MotionSense-v1.0-debug.apk`
- **Release APK**: `android/app/build/outputs/apk/release/app-release.apk`
- **Export Script**: `scripts/export_models_for_mobile.py`
- **Model Assets**: `android/app/src/main/assets/models/`
- **Source Code**: `android/app/src/main/kotlin/com/motionsense/har/`

### 8.2 Installation via ADB
To install directly to a connected Android phone or emulator:
```bash
adb install -r artifacts/apk/MotionSense-v1.0-debug.apk
```

### 8.3 Manual Installation
1. Transfer `MotionSense-v1.0-debug.apk` to your Android phone via USB, Google Drive, or local download.
2. Tap the `.apk` file in your phone's File Manager and allow "Install from Unknown Sources".
3. Open **MotionSense**, grant the Notification and Activity Recognition permissions, and tap **Start Tracking**.
4. Move around (walk, climb stairs, sit, or lie down) to observe real-time classification.
