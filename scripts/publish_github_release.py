import os
import sys
import json
import subprocess
import urllib.request
import urllib.error

def get_github_token():
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        try:
            url = subprocess.check_output(["git", "remote", "get-url", "origin"], text=True).strip()
            if "@" in url:
                creds = url.split("@")[0].split("//")[1]
                if ":" in creds:
                    token = creds.split(":")[1]
        except Exception:
            pass
    return token

GITHUB_TOKEN = get_github_token()
REPO = "leelaprasad6629/human-activity-recognition"
TAG = "v1.0.0-android"
RELEASE_NAME = "MotionSense v1.0.0 Native Android App"

RELEASE_BODY = """## MotionSense — Human Activity Recognition Native Android Application (v1.0.1)

### 📱 Deliverables
- **Debug APK**: `MotionSense-v1.0-debug.apk` (82.53 MB)
  - SHA-256: `6dc8c6cd3a8dac960943a1760748c783c57519a64c9bb295480c3ccfe57dec9f`
  - Signed with: Android Debug Keystore
- **Release APK**: `MotionSense-v1.0-release.apk` (75.47 MB)
  - SHA-256: `b60598a78c58927c9e2ba366c0c235b82c0b1f9ffb3aa435d677a8c6da371f1b`
  - Signed with: Debug Keystore (Configured for immediate physical device testing)
- **Application ID**: `com.motionsense.har`
- **Version**: `1.0` (VersionCode: `1`)
- **Target SDK**: Android 16 (API 36)
- **Min SDK**: Android 8.0 Oreo (API 26)

### 🚀 Simplified & Handheld Real-World Activity Recognition
- **Rotation-Invariant 2-Class Model (WALKING vs STATIONARY)**:
  - 99.86% subject-independent test accuracy, 100.0% walking recall (1,387/1,387 test windows).
  - 3D Euclidean magnitude norms (||v||) make inference completely invariant to vertical phone orientation and tilt in hand.
  - Fixes false `LAYING` and false `WALKING_UPSTAIRS` classifications.
- **3-Class Model (WALKING vs SITTING vs STANDING)**:
  - 87.43% test accuracy on UCI HAR. Documents physical hand ambiguity where sitting and standing produce identical stationary gravity vectors.
- **Ground-Truth Sensor Data Recorder**:
  - Live 50 Hz CSV logging of Accelerometer (X, Y, Z), Gyroscope (X, Y, Z), timestamps, sampling frequency, orientation, and activity label.
  - In-app file management and native Android share intent for instant export.
- **Calibrated Uncertainty & Temporal Smoothing**:
  - Unanimous/majority rolling smoothing over 3 consecutive windows (~3.84s).
  - Calibrated confidence thresholding emits `UNKNOWN ("Detecting...")` state during transitions.
"""

def make_request(url, method="GET", data=None, headers=None):
    if headers is None:
        headers = {}
    headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"
    headers["Accept"] = "application/vnd.github+json"
    headers["User-Agent"] = "MotionSense-Release-Script"
    
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()

def main():
    # 1. Check if release already exists
    print(f"Checking if release {TAG} exists...")
    status, body = make_request(f"https://api.github.com/repos/{REPO}/releases/tags/{TAG}")
    release_id = None
    upload_url = None
    
    if status == 200:
        data = json.loads(body.decode("utf-8"))
        release_id = data["id"]
        upload_url = data["upload_url"].split("{")[0]
        print(f"Release already exists with ID: {release_id}. Updating release details...")
        patch_payload = json.dumps({
            "name": RELEASE_NAME,
            "body": RELEASE_BODY
        }).encode("utf-8")
        make_request(
            f"https://api.github.com/repos/{REPO}/releases/{release_id}",
            method="PATCH",
            data=patch_payload,
            headers={"Content-Type": "application/json"}
        )
    else:
        print("Creating release...")
        payload = json.dumps({
            "tag_name": TAG,
            "target_commitish": "feature/native-android-app",
            "name": RELEASE_NAME,
            "body": RELEASE_BODY,
            "draft": False,
            "prerelease": False
        }).encode("utf-8")
        status, body = make_request(
            f"https://api.github.com/repos/{REPO}/releases",
            method="POST",
            data=payload,
            headers={"Content-Type": "application/json"}
        )
        if status not in (200, 201):
            print(f"Failed to create release: {status} {body.decode('utf-8')}")
            sys.exit(1)
        data = json.loads(body.decode("utf-8"))
        release_id = data["id"]
        upload_url = data["upload_url"].split("{")[0]
        print(f"Created release with ID: {release_id}")
        
    # 2. Upload APK assets
    assets = [
        ("MotionSense-v1.0-debug.apk", "artifacts/apk/MotionSense-v1.0-debug.apk"),
        ("MotionSense-v1.0-release.apk", "artifacts/apk/MotionSense-v1.0-release.apk")
    ]
    
    # Check existing assets
    status, body = make_request(f"https://api.github.com/repos/{REPO}/releases/{release_id}/assets")
    existing_assets = {}
    if status == 200:
        for a in json.loads(body.decode("utf-8")):
            existing_assets[a["name"]] = a["id"]
            
    uploaded_urls = {}
    for name, path in assets:
        if name in existing_assets:
            print(f"Asset {name} already exists. Deleting first...")
            make_request(
                f"https://api.github.com/repos/{REPO}/releases/assets/{existing_assets[name]}",
                method="DELETE"
            )
            
        print(f"Uploading {name} ({os.path.getsize(path) / (1024*1024):.2f} MB)...")
        with open(path, "rb") as f:
            data = f.read()
            
        url = f"{upload_url}?name={name}"
        headers = {
            "Content-Type": "application/vnd.android.package-archive",
            "Content-Length": str(len(data))
        }
        status, body = make_request(url, method="POST", data=data, headers=headers)
        if status in (200, 201):
            res_json = json.loads(body.decode("utf-8"))
            download_url = res_json.get("browser_download_url")
            print(f"Successfully uploaded {name} -> {download_url}")
            uploaded_urls[name] = download_url
        else:
            print(f"Failed to upload {name}: {status} {body.decode('utf-8')}")
            
    print("\nRelease Summary:")
    print(f"Release URL: https://github.com/{REPO}/releases/tag/{TAG}")
    for k, v in uploaded_urls.items():
        print(f"{k}: {v}")

if __name__ == "__main__":
    main()
