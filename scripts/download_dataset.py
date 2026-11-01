#!/usr/bin/env python3
"""
Official UCI HAR Dataset 240 Downloader and Extractor
Dataset: Human Activity Recognition Using Smartphones
UCI ID: 240
URL: https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones
"""

import os
import sys
import zipfile
import urllib.request
import urllib.error
import hashlib
import json
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "ml" / "data" / "raw"
DATA_DIR.mkdir(parents=True, exist_ok=True)

# Official UCI download endpoints
PRIMARY_URL = "https://archive.ics.uci.edu/static/public/240/human+activity+recognition+using+smartphones.zip"
FALLBACK_URL = "https://archive.ics.uci.edu/ml/machine-learning-databases/00240/UCI%20HAR%20Dataset.zip"

ZIP_PATH = DATA_DIR / "uci_har_dataset.zip"
TARGET_DIR = DATA_DIR / "UCI HAR Dataset"

def download_file(url: str, dest: Path) -> bool:
    print(f"[*] Attempting download from: {url}")
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MotionSense/1.0"}
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as response, open(dest, "wb") as out_file:
            total_size = int(response.info().get("Content-Length", -1))
            downloaded = 0
            chunk_size = 1024 * 512
            while True:
                chunk = response.read(chunk_size)
                if not chunk:
                    break
                downloaded += len(chunk)
                out_file.write(chunk)
                if total_size > 0:
                    percent = (downloaded / total_size) * 100
                    sys.stdout.write(f"\r    -> Downloaded: {downloaded / (1024*1024):.2f} MB / {total_size / (1024*1024):.2f} MB ({percent:.1f}%)")
                else:
                    sys.stdout.write(f"\r    -> Downloaded: {downloaded / (1024*1024):.2f} MB")
                sys.stdout.flush()
        print("\n[+] Download completed successfully.")
        return True
    except Exception as e:
        print(f"\n[-] Failed downloading from {url}: {e}")
        if dest.exists():
            dest.unlink()
        return False

def extract_and_verify(zip_file: Path, extract_to: Path):
    print(f"[*] Extracting {zip_file} to {extract_to}...")
    with zipfile.ZipFile(zip_file, "r") as zf:
        zf.extractall(extract_to)
    
    # Sometimes UCI zips contain an inner zip (e.g. UCI HAR Dataset.zip)
    inner_zip = extract_to / "UCI HAR Dataset.zip"
    if inner_zip.exists():
        print("[*] Found nested archive: extracting inner zip...")
        with zipfile.ZipFile(inner_zip, "r") as zf:
            zf.extractall(extract_to)
        inner_zip.unlink()

    print("[+] Extraction finished.")

def main():
    if TARGET_DIR.exists() and (TARGET_DIR / "features.txt").exists():
        print(f"[+] Dataset already present at: {TARGET_DIR}")
        return 0

    success = False
    for url in [PRIMARY_URL, FALLBACK_URL]:
        if download_file(url, ZIP_PATH):
            success = True
            break
    
    if not success:
        print("[-] Error: Unable to download dataset from official sources.")
        return 1

    extract_and_verify(ZIP_PATH, DATA_DIR)
    
    if (TARGET_DIR / "features.txt").exists():
        print(f"[+] Dataset successfully verified at {TARGET_DIR}")
        return 0
    else:
        print(f"[-] Warning: Extracted directory layout needs adjustment. Checked: {TARGET_DIR}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
