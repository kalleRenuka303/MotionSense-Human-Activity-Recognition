# Research Limitations & Future Scope

## 1. Scientific & Practical Limitations

### A. Waist-Mounted Sensor Positioning
- The primary UCI HAR Dataset 240 was recorded with a Samsung Galaxy S II mounted at the subject's waist.
- In everyday smartphone usage, devices are placed in trouser pockets, backpacks, jacket pockets, or held in hands. Orientations differ from waist-mounted gravity alignments and require coordinate rotation invariance or quaternion pre-transforms.

### B. Fixed 50 Hz Sampling Frequency
- The models expect 128 readings representing $\approx 2.56$ seconds at 50 Hz.
- Smartphones operating at variable sampling frequencies (e.g. 20 Hz battery saver or 100 Hz high performance) must be resampled using linear interpolation or cubic spline interpolation before model feeding.

### C. Class Set Boundaries
- The platform recognizes six predefined physical activities: `WALKING`, `WALKING_UPSTAIRS`, `WALKING_DOWNSTAIRS`, `SITTING`, `STANDING`, `LAYING`.
- Unseen transitional activities (e.g., sit-to-stand, stand-to-sit, running, cycling) will either be flagged as uncertain or mapped to the closest kinetic neighbor.

---

## 2. Responsible AI & Healthcare Disclaimer
- MotionSense is designed for academic research, athletic telemetry, and sports science exploration.
- It is **not** a certified medical diagnostic device and must not be used as clinical diagnostic equipment.

---

## 3. Future Research Extensions

### A. Postural Transitions (UCI HAR Dataset 341)
- Future work may incorporate UCI Dataset 341 (Human Activities and Postural Transitions), adding 6 transition classes:
  - `STAND_TO_SIT`, `SIT_TO_STAND`, `SIT_TO_LIE`, `LIE_TO_SIT`, `STAND_TO_LIE`, `LIE_TO_STAND`.

### B. Self-Supervised Pretraining
- Implement contrastive learning (SimCLR for Time Series) on unlabelled smartphone accelerometer streams before supervised fine-tuning.

### C. On-Device Edge Inference
- Export PyTorch checkpoints to ONNX and TensorFlow Lite (TFLite) for zero-latency inference directly inside native iOS (CoreML) and Android (NNAPI) runtimes.
