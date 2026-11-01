# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

Security is paramount for edge mobile applications and sensor-based systems. If you discover a security vulnerability or credential leak within this repository:

1. **Do not create a public GitHub issue.**
2. Send a report directly to the repository maintainer or open a private GitHub Security Advisory.
3. Please include:
   - Detailed description of the vulnerability.
   - Steps to reproduce or proof-of-concept.
   - Potential impact on client devices or backend infrastructure.
4. We acknowledge vulnerability reports promptly and provide coordinated fixes and releases.

---

## Edge Privacy & Permissions

MotionSense adheres to the principle of least privilege:
- Sensor data is processed entirely **on-device** using local ONNX Runtime inference.
- No continuous audio, video, or location (GPS) permissions are requested.
- Accelerometer and gyroscope data remain on the local device unless the user explicitly uses the in-app export feature.
