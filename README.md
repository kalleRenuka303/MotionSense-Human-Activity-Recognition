# MotionSense — Human Activity Recognition Using Smartphones

[![UCI HAR Dataset](https://img.shields.io/badge/Dataset-UCI%20HAR%20240-blue.svg)](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones)
[![Python](https://img.shields.io/badge/Python-3.9%2B-blue.svg)](https://www.python.org/)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.0%2B-ee4c2c.svg)](https://pytorch.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110%2B-009688.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18%2B-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0%2B-3178c6.svg)](https://www.typescriptlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

MotionSense is an end-to-end, reproducible, research-grade Human Activity Recognition (HAR) platform built on the official **UCI Human Activity Recognition Using Smartphones Dataset (ID 240)**. 

The system processes real tri-axial smartphone inertial sensor measurements (accelerometer and gyroscope) to recognize six physical human activities without relying on cameras or external wearables.

---

## 🎯 Target Physical Activities

| ID | Activity Label | Description |
|:---|:---|:---|
| **1** | `WALKING` | Dynamic periodic forward locomotion on level ground |
| **2** | `WALKING_UPSTAIRS` | Ascending stairs (increased vertical acceleration and body gyro energy) |
| **3** | `WALKING_DOWNSTAIRS` | Descending stairs (distinct gravitational impact profiles) |
| **4** | `SITTING` | Static posture, body trunk upright with legs bent |
| **5** | `STANDING` | Static posture, vertical alignment with subtle sway |
| **6** | `LAYING` | Static horizontal posture, distinct gravity vector realignment |
