# Official Dataset Documentation: UCI HAR Dataset 240

## 1. Provenance & Citations
- **Full Title**: Human Activity Recognition Using Smartphones Dataset
- **Repository**: UCI Machine Learning Repository (Dataset ID: 240)
- **Official URL**: [https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones](https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones)
- **Principal Authors**: Davide Anguita, Alessandro Ghio, Luca Oneto, Xavier Parra, and Jorge L. Reyes-Ortiz (ESANN 2013).

---

## 2. Experimental Data Collection Protocol
- **Subject Population**: 30 healthy human volunteers aged between 19 and 48 years.
- **Hardware Instrument**: Samsung Galaxy S II smartphone secured at the user's waist.
- **Embedded Sensors**:
  - Tri-axial Accelerometer (measuring acceleration in units of gravitational acceleration $g$).
  - Tri-axial Gyroscope (measuring angular velocity in radians per second $\text{rad/s}$).
- **Sampling Frequency**: Constant 50 Hz ($20\text{ ms}$ interval between consecutive readings).
- **Video Ground Truth**: Experiments were video-recorded to facilitate manual labeling of each activity segment.

---

## 3. Audited Dataset Statistics
From our automated verification suite (`scripts/verify_dataset.py`):

| Statistic | Value | Verification Status |
|:---|:---|:---|
| **Total Windows** | 10,299 | PASS (Exact match) |
| **Training Partition (70%)** | 7,352 windows | PASS |
| **Testing Partition (30%)** | 2,947 windows | PASS |
| **Distinct Subjects** | 30 volunteers | PASS |
| **Train Subjects** | 21 subjects ([1, 3, 5, 6, 8, 11, 14, 15, 16, 17, 19, 21, 22, 23, 25, 26, 27, 28, 29, 30]) | PASS |
| **Test Subjects** | 9 subjects ([2, 4, 9, 10, 12, 13, 18, 20, 24]) | PASS |
| **Subject Overlap (Leakage)** | 0 subjects | PASS (Zero leakage verified) |
| **Window Length** | 128 readings ($\approx 2.56$ seconds) | PASS |
| **Window Overlap** | 50% (64 readings shift) | PASS |
| **Statistical Features** | 561 columns (`features.txt`) | PASS |
| **Raw Inertial Channels** | 9 channels (`Inertial Signals/`) | PASS |

---

## 4. Exact Six Activity Classes

| ID | Class Label | Activity Category | Total Windows | Dynamic Characteristics |
|:---|:---|:---|:---|:---|
| **1** | `WALKING` | Dynamic Locomotion | 1,722 | Periodic gait oscillations along $x$ and $y$ planes |
| **2** | `WALKING_UPSTAIRS` | Dynamic Locomotion | 1,544 | Elevated vertical impact peaks and increased gyroscope energy |
| **3** | `WALKING_DOWNSTAIRS` | Dynamic Locomotion | 1,406 | Pronounced heel-strike decelerations and asymmetric vertical motion |
| **4** | `SITTING` | Static Posture | 1,777 | Steady gravity alignment along vertical trunk axis, bent lower limbs |
| **5** | `STANDING` | Static Posture | 1,906 | Vertical orientation with low-frequency postural sway |
| **6** | `LAYING` | Static Posture | 1,944 | Orthogonal gravity rotation (gravity shifts from $y$ to $x/z$ axis) |

---

## 5. Directory Structure
```
UCI HAR Dataset/
├── activity_labels.txt      # 6 activity identifiers and labels
├── features.txt             # 561 feature column headers
├── features_info.txt        # Explanation of time/frequency transformations
├── README.txt               # Original authors' release documentation
├── train/
│   ├── X_train.txt          # (7352, 561) feature matrix
│   ├── y_train.txt          # (7352, 1) target labels (1..6)
│   ├── subject_train.txt    # (7352, 1) subject identifiers
│   └── Inertial Signals/    # 9 files of shape (7352, 128)
└── test/
    ├── X_test.txt           # (2947, 561) feature matrix
    ├── y_test.txt           # (2947, 1) target labels (1..6)
    ├── subject_test.txt     # (2947, 1) subject identifiers
    └── Inertial Signals/    # 9 files of shape (2947, 128)
```
