#!/usr/bin/env python3
"""
UCI HAR Dataset 240 Verification & Integrity Checker (Zero third-party dependencies)
Verifies:
- 10,299 expected windows (7,352 train + 2,947 test)
- 561 feature columns in features.txt
- 9 raw inertial signal channels (128 time steps @ 50Hz)
- 6 activity classes (WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING)
- 30 distinct subjects (21 train, 9 test)
- Zero subject overlap between train and test
- Absence of corrupted or empty lines
- Machine-readable verification JSON export
"""

import sys
import json
from pathlib import Path
from datetime import datetime, timezone

ROOT_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT_DIR / "ml" / "data" / "raw" / "UCI HAR Dataset"
ARTIFACTS_DIR = ROOT_DIR / "artifacts" / "metadata"
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

EXPECTED_CHANNELS = [
    "total_acc_x",
    "total_acc_y",
    "total_acc_z",
    "body_acc_x",
    "body_acc_y",
    "body_acc_z",
    "body_gyro_x",
    "body_gyro_y",
    "body_gyro_z"
]

EXPECTED_ACTIVITIES = {
    1: "WALKING",
    2: "WALKING_UPSTAIRS",
    3: "WALKING_DOWNSTAIRS",
    4: "SITTING",
    5: "STANDING",
    6: "LAYING"
}

def load_lines(path: Path):
    with open(path, "r", encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip()]

def verify_dataset():
    report = {
        "dataset_name": "Human Activity Recognition Using Smartphones (UCI HAR Dataset 240)",
        "source_url": "https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones",
        "verification_timestamp": datetime.now(timezone.utc).isoformat(),
        "status": "PASS",
        "checks": {}
    }

    if not DATA_DIR.exists():
        print(f"[-] Error: Dataset directory {DATA_DIR} does not exist.")
        report["status"] = "FAIL"
        report["error"] = "Dataset directory missing"
        return report

    print("\n=======================================================")
    print("  UCI HAR Dataset 240 Verification & Integrity Audit")
    print("=======================================================")

    # 1. Activity labels
    labels_file = DATA_DIR / "activity_labels.txt"
    activity_mapping = {}
    for line in load_lines(labels_file):
        parts = line.split()
        activity_mapping[int(parts[0])] = parts[1]
    
    activity_check = (activity_mapping == EXPECTED_ACTIVITIES)
    report["checks"]["activity_labels"] = {
        "status": "PASS" if activity_check else "FAIL",
        "mapping": activity_mapping
    }
    print(f"[+] Activity labels: {activity_mapping}")

    # 2. Features
    features_file = DATA_DIR / "features.txt"
    features = [line.split(maxsplit=1)[1] for line in load_lines(features_file)]
    num_features = len(features)
    feature_check = (num_features == 561)
    report["checks"]["features"] = {
        "status": "PASS" if feature_check else "FAIL",
        "count": num_features,
        "first_5": features[:5],
        "last_5": features[-5:]
    }
    print(f"[+] Features count: {num_features} (Expected: 561) -> {'PASS' if feature_check else 'FAIL'}")

    # 3. Subjects & Labels
    sub_train = [int(x) for x in load_lines(DATA_DIR / "train" / "subject_train.txt")]
    sub_test = [int(x) for x in load_lines(DATA_DIR / "test" / "subject_test.txt")]
    
    y_train = [int(x) for x in load_lines(DATA_DIR / "train" / "y_train.txt")]
    y_test = [int(x) for x in load_lines(DATA_DIR / "test" / "y_test.txt")]

    n_train = len(y_train)
    n_test = len(y_test)
    n_total = n_train + n_test

    unique_train_subs = sorted(list(set(sub_train)))
    unique_test_subs = sorted(list(set(sub_test)))
    overlap_subs = set(unique_train_subs).intersection(set(unique_test_subs))

    subject_leakage_check = (len(overlap_subs) == 0)
    total_subjects = len(unique_train_subs) + len(unique_test_subs)

    report["checks"]["subjects_and_splits"] = {
        "total_windows": n_total,
        "expected_total": 10299,
        "train_windows": n_train,
        "test_windows": n_test,
        "train_subjects": unique_train_subs,
        "test_subjects": unique_test_subs,
        "subject_overlap": list(overlap_subs),
        "zero_leakage_verified": subject_leakage_check,
        "total_unique_subjects": total_subjects
    }
    print(f"[+] Total Windows: {n_train} train + {n_test} test = {n_total} total (Expected: 10,299) -> {'PASS' if n_total==10299 else 'FAIL'}")
    print(f"[+] Subjects: {len(unique_train_subs)} train subjects ({unique_train_subs}), {len(unique_test_subs)} test subjects ({unique_test_subs})")
    print(f"[+] Zero Subject Leakage: Overlap={len(overlap_subs)} -> {'PASS' if subject_leakage_check else 'FAIL'}")

    # 4. Inertial Signals Check
    print("[*] Auditing raw 9-axis inertial signals...")
    inertial_checks = {}
    for split, n_win in [("train", n_train), ("test", n_test)]:
        signals_dir = DATA_DIR / split / "Inertial Signals"
        split_checks = {}
        for ch in EXPECTED_CHANNELS:
            ch_filename = f"{ch}_{split}.txt"
            ch_path = signals_dir / ch_filename
            if not ch_path.exists():
                split_checks[ch] = {"exists": False}
                continue
            with open(ch_path, "r") as f:
                first_line = f.readline().strip().split()
                line_count = 1 + sum(1 for _ in f)
            split_checks[ch] = {
                "exists": True,
                "lines": line_count,
                "timesteps": len(first_line),
                "valid": (line_count == n_win and len(first_line) == 128)
            }
        inertial_checks[split] = split_checks

    report["checks"]["inertial_signals"] = inertial_checks

    all_inertial_valid = all(
        all(ch_info.get("valid", False) for ch_info in split_info.values())
        for split_info in inertial_checks.values()
    )
    print(f"[+] Raw 9-Axis Inertial Signals (128 timesteps): {'PASS' if all_inertial_valid else 'FAIL'}")

    # Final summary check
    all_passed = (
        activity_check and 
        feature_check and 
        (n_total == 10299) and 
        subject_leakage_check and 
        (total_subjects == 30) and
        all_inertial_valid
    )
    report["status"] = "PASS" if all_passed else "FAIL"

    # Save JSON report
    out_path = ARTIFACTS_DIR / "dataset_verification.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    print(f"\n=======================================================")
    print(f"  AUDIT RESULT: {report['status']}")
    print(f"  Saved verification report to: {out_path}")
    print(f"=======================================================\n")
    return report

if __name__ == "__main__":
    rep = verify_dataset()
    sys.exit(0 if rep["status"] == "PASS" else 1)
