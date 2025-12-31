    # AuraLink 🎧

**AuraLink** is a local-only, privacy-focused Android application designed to announce incoming caller names through your Bluetooth headset. Never take your phone out of your pocket again while driving or working.

## ✨ Features

- **Local Caller Announcement**: Uses Text-to-Speech (TTS) to announce contacts. No internet required.
- **Driving Mode**: Automatically active for a user-defined duration (15-120 mins) with an auto-expiry timer.
- **Always Active Mode**: Keep the service running persistently for maximum convenience.
- **Bluetooth Routing**: Smart audio routing ensures announcements only play when a Bluetooth device is connected.
- **Quick Settings Integration**: Toggle modes directly from your Android notification shade.
- **Privacy First**: 100% offline. No data leaves your device.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Foreground Service for reliable background processing.

## 🚀 Getting Started

### Prerequisites
- Android 9.0 (API 28) or higher.
- Bluetooth headset connected.

### Permissions Required
- `READ_CONTACTS`: To identify who is calling.
- `READ_PHONE_STATE` & `READ_CALL_LOG`: To detect incoming calls.
- `BLUETOOTH_CONNECT`: To detect your headset.
- `POST_NOTIFICATIONS`: To display the active service status.

### Building
```powershell
./gradlew installDebug
```

## 📋 How to Use
1. Open the app and grant the required permissions.
2. Connect your Bluetooth headset.
3. Enable "Always Active" or "Driving Mode" from the Dashboard or Quick Settings.
4. Set your preferred Speech Volume and Announcement Delay in Settings.

---
*Developed with privacy and safety in mind.*
