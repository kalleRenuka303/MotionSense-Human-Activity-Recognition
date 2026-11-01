import os
import sys
import json
import urllib.request
import urllib.error

GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")
REPO = "leelaprasad6629/human-activity-recognition"
TAG = "v1.0.0-android"
RELEASE_NAME = "MotionSense v1.0.0 Native Android App"

RELEASE_BODY = """## MotionSense — Human Activity Recognition Native Android Application

### 📱 Deliverables
- **Debug APK**: `MotionSense-v1.0-debug.apk` (82.53 MB)
  - SHA-256: `7b0a46052a61f89c502f0a76f18d54971a5848b337e8cead65d5bc32d3f8b84b`
  - Signed with: Android Debug Keystore
- **Release APK**: `MotionSense-v1.0-release.apk` (75.47 MB)
  - SHA-256: `512f61f4467c1e3f4c4983adc3c25454580e91784f209ff8d460d77a7d650dbe`
  - Signed with: Debug Keystore (Configured for immediate physical device testing)
- **Application ID**: `com.motionsense.har`
- **Version**: `1.0` (VersionCode: `1`)
- **Target SDK**: Android 16 (API 36)
- **Min SDK**: Android 8.0 Oreo (API 26)

### 🚀 Highlights
- **100% Native Kotlin & Jetpack Compose (Material 3)**.
- **Real-time 50 Hz Sensor Pipeline**: 9 synchronized channels (`total_acc`, `body_acc`, `body_gyro`).
- **On-Device ONNX Runtime**: CNN-LSTM (96.06% accuracy) and 1D CNN (95.76% accuracy).
- **Foreground Tracking Service**: Continuous activity classification with screen off and notifications.
- **Local SQLite History Database**: Full local logging with real-time reactive UI flows.
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
        print(f"Release already exists with ID: {release_id}")
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
