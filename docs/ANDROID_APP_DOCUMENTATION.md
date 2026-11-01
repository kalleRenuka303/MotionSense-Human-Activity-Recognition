# MotionSense — Native Android Application Technical Documentation & Architecture

## 1. Executive Summary & Real-World Handheld Optimization

During real-world physical smartphone testing of the initial native Android build, physical **WALKING** was consistently misclassified as **LAYING** or confused with stairs when held vertically in the hand. A thorough technical audit spanning dataset capture protocols, digital signal processing, coordinate systems, and neural network activations isolated the exact root cause and delivered a robust solution:

### 1.1 The Root Cause: Spatial Coordinate Frame Inversion
1. **The Original UCI HAR Dataset Setup**:
   In the original UCI HAR experiments (Anguita et al.), subjects wore a smartphone mounted **horizontally on the waist** (in a horizontal belt holster).
   - In this waist-mounted landscape orientation, Earth's gravity vector ($\approx +1.0 g$) was aligned with the smartphone's **$X$-axis** for all upright activities:
     - `WALKING`: $\overline{a_x} = +0.9997 g, \sigma = 0.2269 g$
     - `STANDING`: $\overline{a_x} = +1.0014 g$
     - `SITTING`: $\overline{a_x} = +0.9508 g$
   - Conversely, the **only** activity in the entire UCI HAR dataset where gravity is absent from the $X$-axis is **LAYING**:
     - `LAYING`: $\overline{a_x} = +0.0717 g, \overline{a_y} = +0.6496 g, \overline{a_z} = +0.5605 g$
2. **The Handheld Smartphone Mismatch**:
   When a user holds a smartphone in vertical portrait mode:
   - Earth's gravity vector is aligned with Android's **$Y$-axis** ($a_y \approx +1.0 g$).
   - The lateral axis across the screen width is Android's **$X$-axis** ($a_x \approx 0.0 g$).
3. **The Consequence**:
   When the unaligned Android sensor readings were fed into the 6-class neural network, $a_x \approx 0.0 g$ triggered `LAYING` predictions with 100% confidence.

---

## 2. Model Simplification & Rotation Invariance

### 2.1 Rotation-Invariant 2-Class Model (`walking_binary.onnx`)
To make activity recognition physically reliable for a phone held vertically or tilted arbitrarily in the user's hand, MotionSense features `walking_binary.onnx`:
- **Classes**: `WALKING` vs `NOT_WALKING` (Stationary)
- **Mathematical Invariance**: Operates on 3D Euclidean magnitude norms:
  $$a_{\text{mag}} = \sqrt{a_x^2 + a_y^2 + a_z^2}, \quad a_{\text{body,mag}} = \sqrt{a_{\text{body},x}^2 + a_{\text{body},y}^2 + a_{\text{body},z}^2}, \quad \omega_{\text{mag}} = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$$
- **Test Set Accuracy**: **99.86%**
- **Walking Recall**: **100.00%** (1,387 / 1,387 test windows)
- **Walking Precision**: **99.71%**
- **Inference Latency**: $< 0.5\text{ ms}$ on mobile CPU (11.0 KB model footprint).

### 2.2 Physical Limitations of Handheld 3-Class Recognition (`three_class.onnx`)
- When an inertial sensor is in the hand, sitting and standing are **physically indistinguishable**:
  - The hand has no mechanical contact with chair or floor.
  - Both postures exhibit identical gravity alignment ($a_y \approx +1g$) and stillness ($\sigma(a) < 0.015g$).
- Model B (`three_class.onnx`) achieves 87.43% test accuracy on UCI HAR, but sitting recall drops to 42.4% due to confusion with standing.
- Therefore, the 2-class model is recommended for handheld testing.

---

## 3. Sensor Preprocessing & Digital Butterworth Filter

### 3.1 Exact 3rd-Order Butterworth Low-Pass Filter
[`ButterworthFilter.kt`](file:///Users/kalagotlashivareddy/Downloads/human%20activity%20recognition/android/app/src/main/kotlin/com/motionsense/har/sensor/ButterworthFilter.kt) implements a 3rd-order digital Butterworth filter ($f_c = 0.3\text{ Hz}, f_s = 50\text{ Hz}$) using Direct Form II Transposed in 64-bit precision:
- **Correlation with UCI HAR ground truth**: $r > 0.92$ across all three axes.
- **Attenuation at Walking Frequency (2 Hz)**: $> 90\%$ attenuation, preventing cyclic walking steps from corrupting the extracted gravity vector.
- **Instant DC Initialization**: Eliminates initial transient filter warm-up delay.

---

## 4. Sensor Data Recorder & Ground-Truth CSV Sessions

[`SensorDataRecorder.kt`](file:///Users/kalagotlashivareddy/Downloads/human%20activity%20recognition/android/app/src/main/kotlin/com/motionsense/har/sensor/SensorDataRecorder.kt) enables recording physical sensor data directly on the device:
- **Format**: Labelled CSV with header:
  `timestamp_ns,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,sampling_rate_hz,orientation,activity_label`
- **Supported Labels**: `WALKING`, `SITTING`, `STANDING`, `WALKING_UPSTAIRS`, `WALKING_DOWNSTAIRS`, `LAYING`.
- **Export & Sharing**: Uses Android `FileProvider` to share CSV files via the native Android share sheet (Gmail, Drive, WhatsApp, etc.).

---

## 5. Temporal Smoothing & Prediction Stability

- **Rolling Majority Vote**: Evaluates predictions over a 3-window circular buffer ($3 \times 1.28\text{ s} \approx 3.84\text{ seconds}$).
- **Calibrated Uncertainty Cutoff**: When top probability is $< 0.60$ for binary or $< 0.50$ for multi-class, the system displays **UNKNOWN ("Detecting...")** and resets history.

---

## 6. APK Deliverables & Checksums

| Field | Debug APK | Release APK |
|---|---|---|
| **Direct Download URL** | [Download Debug APK](https://github.com/leelaprasad6629/human-activity-recognition/releases/download/v1.0.0-android/MotionSense-v1.0-debug.apk) | [Download Release APK](https://github.com/leelaprasad6629/human-activity-recognition/releases/download/v1.0.0-android/MotionSense-v1.0-release.apk) |
| **Package ID** | `com.motionsense.har` | `com.motionsense.har` |
| **Version** | `1.0` (Code: `1`) | `1.0` (Code: `1`) |
| **File Size** | $82.53\text{ MB}$ | $75.49\text{ MB}$ |
| **SHA-256 Checksum** | `6dc8c6cd3a8dac960943a1760748c783c57519a64c9bb295480c3ccfe57dec9f` | `b60598a78c58927c9e2ba366c0c235b82c0b1f9ffb3aa435d677a8c6da371f1b` |
| **Signing Key** | Debug Keystore | Debug Keystore (Configured for direct side-loading) |
