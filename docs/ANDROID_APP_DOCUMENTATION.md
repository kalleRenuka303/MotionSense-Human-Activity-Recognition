# MotionSense — Native Android Application Technical Documentation & Bugfix Audit

## 1. Executive Summary & Critical Root Cause Analysis

During real-world physical smartphone testing of the initial native Android build, physical **WALKING** was consistently misclassified as **LAYING** with near 100% confidence. A thorough technical audit spanning dataset capture protocols, digital signal processing, coordinate systems, and neural network activations isolated the exact root cause:

### The Root Cause: Spatial Coordinate Frame Inversion
1. **The Original UCI HAR Dataset Setup**:
   In the original UCI HAR experiments (Anguita et al.), subjects wore a smartphone mounted **horizontally on the waist** (in a horizontal belt holster).
   - In this waist-mounted landscape orientation, Earth's gravity vector ($\approx +1.0 g$) was aligned with the smartphone's **$X$-axis** for all upright activities:
     - `WALKING`: $\overline{a_x} = +0.9997 g, \sigma = 0.2269 g$
     - `WALKING_UPSTAIRS`: $\overline{a_x} = +0.9494 g$
     - `STANDING`: $\overline{a_x} = +1.0014 g$
     - `SITTING`: $\overline{a_x} = +0.9508 g$
   - Conversely, the **only** activity in the entire UCI HAR dataset where gravity is absent from the $X$-axis is **LAYING**:
     - `LAYING`: $\overline{a_x} = +0.0717 g, \overline{a_y} = +0.6496 g, \overline{a_z} = +0.5605 g$
2. **The Physical Android Phone Mismatch**:
   When a user holds a smartphone in normal portrait mode or places it in a front trouser pocket (vertical, top facing up):
   - Earth's gravity vector is aligned with Android's **$Y$-axis** ($a_y \approx +1.0 g$).
   - The lateral axis across the screen width is Android's **$X$-axis** ($a_x \approx 0.0 g$).
3. **The Consequence**:
   When the unaligned Android sensor readings were fed directly into the neural network:
   - Channel 0 (`total_acc_x`) received $\approx 0.0 g$.
   - Channel 1 (`total_acc_y`) received $\approx 1.0 g$.
   - The neural network's trained convolutional filters interpreted $a_x \approx 0.0 g$ as conclusive evidence that the body is horizontal, immediately predicting **LAYING (100% confidence)**!
4. **The Mathematical Verification**:
   When tested on real UCI HAR test windows in Python:
   - Normal waist orientation: 10/10 walking windows predict `WALKING (100.0%)`.
   - Portrait orientation ($X \leftrightarrow Y$ unaligned): 10/10 walking windows predict `LAYING (100.0%)`.
   - With the coordinate transformation layer applied: **10/10 walking windows predict `WALKING (100.0%)`**.

---

## 2. Sensor Preprocessing & Butterworth Filter Fix

### 2.1 The Butterworth 0.3 Hz Low-Pass Filter
The previous implementation used a simple single-pole IIR filter:
$$y_t = \alpha y_{t-1} + (1 - \alpha) x_t \quad (\alpha = 0.8)$$
At 50 Hz sampling rate, $\alpha = 0.8$ produces an effective cutoff frequency of:
$$f_c = \frac{-\ln(0.8)}{2\pi \cdot 0.02} \approx 1.77\text{ Hz}$$
Because normal human walking strides occur between $1.5\text{ Hz}$ and $2.5\text{ Hz}$, this filter treated cyclic walking steps as gravity, subtracting them from total acceleration and obliterating the walking waveform in `body_acc`.

### 2.2 The Exact 3rd-Order Butterworth Filter Implementation
To match the exact signal processing pipeline of the original UCI HAR dataset, [`ButterworthFilter3rdOrder.kt`](file:///Users/kalagotlashivareddy/Downloads/human%20activity%20recognition/android/app/src/main/kotlin/com/motionsense/har/sensor/ButterworthFilter.kt) implements a 3rd-order low-pass Butterworth filter ($f_c = 0.3\text{ Hz}, f_s = 50\text{ Hz}$) using Direct Form II Transposed in 64-bit IEEE 754 `Double` precision:

- **Filter Coefficients** (generated via `scipy.signal.butter(3, 0.3 / 25.0, btype='low')`):
  $$b = [6.451849 \times 10^{-6}, 1.935555 \times 10^{-5}, 1.935555 \times 10^{-5}, 6.451849 \times 10^{-6}]$$
  $$a = [1.0, -2.924606, 2.852028, -0.927370]$$
- **Verification on UCI HAR Ground Truth**:
  - $X$-axis correlation with original UCI HAR `body_acc_x`: **$0.9279$** ($\text{MAE} = 0.0117 g$).
  - $Y$-axis correlation with original UCI HAR `body_acc_y`: **$0.9206$** ($\text{MAE} = 0.0119 g$).
  - $Z$-axis correlation with original UCI HAR `body_acc_z`: **$0.9262$** ($\text{MAE} = 0.0095 g$).
- **Instant DC Initialization**: The filter states ($w_1, w_2, w_3$) are initialized to the DC steady-state for the initial acceleration magnitude, eliminating startup transient settling time.

---

## 3. Phone Placement & Coordinate Alignment Layer

The app provides a runtime **Placement Selector** in [`PhonePlacement.kt`](file:///Users/kalagotlashivareddy/Downloads/human%20activity%20recognition/android/app/src/main/kotlin/com/motionsense/har/sensor/PhonePlacement.kt):

| Placement Mode | Physical Phone Location | Sensor-to-Model Coordinate Transformation |
|---|---|---|
| **Front Pocket (Portrait)** *(Default)* | Vertical in front trouser pocket, top facing UP, screen facing thigh | $X_{\text{model}} = +Y_{\text{phone}}$ (Vertical Gravity)<br>$Y_{\text{model}} = -Z_{\text{phone}}$ (Forward Walk Direction)<br>$Z_{\text{model}} = +X_{\text{phone}}$ (Lateral Sway) |
| **Waist Holster (UCI Native)** | Horizontally mounted on waist belt | $X_{\text{model}} = X_{\text{phone}}$<br>$Y_{\text{model}} = Y_{\text{phone}}$<br>$Z_{\text{model}} = Z_{\text{phone}}$ |
| **Handheld (Portrait)** | Held in front of user while walking, tilted $\approx 30^\circ$ forward | $X_{\text{model}} = 0.866 Y - 0.500 Z$<br>$Y_{\text{model}} = -0.866 Z - 0.500 Y$<br>$Z_{\text{model}} = X_{\text{phone}}$ |

---

## 4. Model Selection & Architecture Comparison

### Why 1D CNN is More Suitable for Mobile HAR than CNN-LSTM:
1. **Accuracy on Unseen Test Subjects**:
   - **1D CNN**: **$93.99\%$** ($2,770 / 2,947$ correct)
   - **CNN-LSTM**: **$90.74\%$** ($2,674 / 2,947$ correct)
   - *1D CNN achieves $+3.25\%$ higher overall accuracy on subject-independent testing.*
2. **Computational Latency & Thermal Footprint**:
   - **1D CNN**: Purely feed-forward 1D convolutions with Batch Normalization and Global Average Pooling. Measured inference latency on mobile CPU is $< 1.5\text{ ms}$.
   - **CNN-LSTM**: Recurrent LSTM cells require sequential step-by-step unrolling across 128 timesteps, taking $4.5\text{ ms}$ to $8.0\text{ ms}$ on mobile CPU.
3. **Binary Asset Footprint**:
   - `cnn_1d.onnx`: **434 KB** (111,238 weights)
   - `cnn_lstm.onnx`: **1,034 KB** (263,814 weights)

The application now defaults to **1D CNN**, while retaining the option to evaluate CNN-LSTM dynamically in the UI.

---

## 5. Temporal Smoothing & Prediction Stability

To prevent erratic label flickering during physical transitions (e.g. stepping or brief pauses):
1. **Rolling Majority Vote**: A 3-window circular history buffer evaluates the most frequent activity over the last $3.84\text{ seconds}$ ($3 \times 1.28\text{ s}$ stride).
2. **Uncertainty Cutoff**: When top softmax probability $< 0.50$, the application displays **"Uncertain Motion"** rather than forcing a low-confidence guess.

---

## 6. Live Sensor & ML Diagnostics Mode (Phase 4)

A dedicated diagnostic overlay can be toggled on the Home screen:
- **Telemetry Readout**: Real-time sampling rate (Hz), timestamp jitter (ms), missing samples count.
- **Gravity & Body Acceleration**: Real-time display of separated gravitational ($g_x, g_y, g_z$) and body ($b_x, b_y, b_z$) components.
- **Channel Min/Max**: Dynamic tracking of minimum and maximum amplitudes across all 9 input channels.
- **Model Internals**: Real-time display of raw model logits and 6-class softmax probabilities with percentage bars.
- **Diagnostic Window Export**: Tapping **"Save Diagnostic Window to File"** exports the active 128×9 float32 window as JSON to `Android/data/com.motionsense.har/files/diagnostics/` for inspection and Python parity validation.

---

## 7. APK Delivery & Direct Download Links

| Field | Debug APK | Release APK |
|---|---|---|
| **Direct Download URL** | [Download Debug APK](https://github.com/leelaprasad6629/human-activity-recognition/releases/download/v1.0.0-android/MotionSense-v1.0-debug.apk) | [Download Release APK](https://github.com/leelaprasad6629/human-activity-recognition/releases/download/v1.0.0-android/MotionSense-v1.0-release.apk) |
| **GitHub Release** | [`v1.0.0-android`](https://github.com/leelaprasad6629/human-activity-recognition/releases/tag/v1.0.0-android) | [`v1.0.0-android`](https://github.com/leelaprasad6629/human-activity-recognition/releases/tag/v1.0.0-android) |
| **Package ID** | `com.motionsense.har` | `com.motionsense.har` |
| **Version** | `1.0` (Code: `1`) | `1.0` (Code: `1`) |
| **File Size** | $82.53\text{ MB}$ | $75.47\text{ MB}$ |
| **SHA-256 Checksum** | `b49d3d5d7e2e788c69ab3947e6c04c03e69b4963f7e20c4f595192a2065eee7a` | `1d135473a2a2c1a4e6b9863141a88396279ad2bfaf6181d54134317f6052d00e` |
| **Signing Key** | Debug Keystore | Debug Keystore (Ready for device side-loading) |
