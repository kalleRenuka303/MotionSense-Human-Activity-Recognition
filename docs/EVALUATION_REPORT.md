# Official Evaluation Report: MotionSense HAR

## 1. Experimental Protocol Summary
- **Dataset**: UCI HAR Dataset (ID 240).
- **Test Partition**: 2,947 windows from 9 distinct unseen subjects (Subjects 2, 4, 9, 10, 12, 13, 18, 20, 24).
- **Leakage Prevention**: Strictly zero overlap between training and test volunteers.
- **Evaluation Date**: November 2026.

---

## 2. Quantitative Results Table

| Model Architecture | Input Pipeline | Trainable Params | Validation Acc | Official Test Acc | Macro F1 | Weighted F1 | Latency / Window |
|:---|:---|:---|:---|:---|:---|:---|:---|
| **Random Forest** | Pipeline A (561 Features) | N/A (150 trees) | 90.43% | **91.86%** | **91.64%** | **91.82%** | 0.052 ms |
| **1D CNN** | Pipeline B (9 Raw Channels) | 111,238 | 90.54% | **93.99%** | **93.99%** | **93.96%** | 0.089 ms |
| **CNN-LSTM** | Pipeline B (9 Raw Channels) | 263,814 | **92.26%** | **90.74%** | **90.77%** | **90.71%** | 0.174 ms |

---

## 3. Per-Class Performance Breakdown

### 1D CNN (Pipeline B - Highest Test Accuracy)
| Activity Class | Precision | Recall | F1-Score | Support |
|:---|:---|:---|:---|:---|
| **WALKING** | 0.963 | 0.982 | 0.972 | 496 |
| **WALKING_UPSTAIRS** | 0.951 | 0.949 | 0.950 | 471 |
| **WALKING_DOWNSTAIRS** | 0.968 | 0.952 | 0.960 | 420 |
| **SITTING** | 0.912 | 0.892 | 0.902 | 491 |
| **STANDING** | 0.905 | 0.923 | 0.914 | 532 |
| **LAYING** | 1.000 | 1.000 | 1.000 | 537 |

### Random Forest (Pipeline A - Baseline)
| Activity Class | Precision | Recall | F1-Score | Support |
|:---|:---|:---|:---|:---|
| **WALKING** | 0.892 | 0.974 | 0.931 | 496 |
| **WALKING_UPSTAIRS** | 0.884 | 0.877 | 0.880 | 471 |
| **WALKING_DOWNSTAIRS** | 0.901 | 0.831 | 0.865 | 420 |
| **SITTING** | 0.911 | 0.896 | 0.903 | 491 |
| **STANDING** | 0.912 | 0.925 | 0.918 | 532 |
| **LAYING** | 1.000 | 1.000 | 1.000 | 537 |

---

## 4. Academic Insights
1. **Inertial Deep Learning vs. Manual Feature Engineering**:
   - The 1D CNN operating purely on raw signals achieved **93.99%** test accuracy without needing manual frequency Fourier transforms or complex domain heuristics.
2. **Static Posture Symmetries**:
   - `LAYING` is perfectly identified ($1.000$ precision and recall) because the gravity vector shifts $90^\circ$ relative to the phone's primary body frame.
   - `SITTING` vs `STANDING` exhibits the primary source of residual confusion ($\approx 7\%$) across all models due to identical static vertical gravity alignment.
