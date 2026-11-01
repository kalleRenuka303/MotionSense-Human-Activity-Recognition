# MotionSense — Human Activity Recognition Using Smartphones

[![UCI HAR Dataset](https://img.shields.io/badge/Dataset-UCI%20HAR%20240-blue.svg)](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones)
[![Frontend Preview](https://img.shields.io/badge/Vercel-Live%20Preview-black?logo=vercel)](https://motionsense-doxfb936z-epitometrc.vercel.app)
[![Vercel App](https://img.shields.io/badge/Vercel-motionsense--har-black?logo=vercel)](https://motionsense-har.vercel.app)
[![CI/CD](https://github.com/leelaprasad6629/human-activity-recognition/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/leelaprasad6629/human-activity-recognition/actions)
[![Python](https://img.shields.io/badge/Python-3.9%2B-blue.svg)](https://www.python.org/)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.0%2B-ee4c2c.svg)](https://pytorch.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110%2B-009688.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18%2B-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0%2B-3178c6.svg)](https://www.typescriptlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

MotionSense is an end-to-end, reproducible, research-grade Human Activity Recognition (HAR) platform built on the official **UCI Human Activity Recognition Using Smartphones Dataset (ID 240)**. 

The system processes real tri-axial smartphone inertial sensor measurements (accelerometer and gyroscope) to recognize six physical human activities without relying on cameras or external wearables.

---

## 🌐 Live Deployments

- **Frontend Live Preview**: [https://motionsense-doxfb936z-epitometrc.vercel.app](https://motionsense-doxfb936z-epitometrc.vercel.app)
- **Frontend Primary Domain**: [https://motionsense-har.vercel.app](https://motionsense-har.vercel.app)
- **Live Backend API (Active Tunnel)**: [https://small-pens-travel.loca.lt/api](https://small-pens-travel.loca.lt/api)
- **Render Deployment Blueprint**: 
  [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/leelaprasad6629/human-activity-recognition/tree/develop)

---

## 📊 Model Benchmark Evaluation (Official Test Set, $N = 2,947$)

| Model Architecture | Pipeline / Input Space | Test Accuracy | Macro F1 | Weighted F1 | Inference Latency | Model Size |
|:---|:---|:---:|:---:|:---:|:---:|:---:|
| **Random Forest Baseline** | Pipeline A (561 Pre-computed Features) | **91.86%** | **91.64%** | 91.81% | 0.012 ms / sample | 5.7 MB |
| **1D CNN** | Pipeline B (9 Raw Inertial Channels, 128 timesteps) | **93.99%** | **93.99%** | 93.99% | 0.434 ms / sample | 1.3 MB |
| **CNN-LSTM (Research Model)** | Pipeline B (9 Raw Inertial Channels, 128 timesteps) | **90.74%** | **90.77%** | 90.74% | 0.424 ms / sample | 3.0 MB |

*Zero Subject Leakage*: Strictly evaluated on 9 unseen subjects (2, 4, 9, 10, 12, 13, 18, 20, 24) using official UCI HAR splits. All metrics independently audited and verified with 0.00% discrepancy.

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
