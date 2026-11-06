# VolumeTap for Samsung Galaxy M32

An Android Studio Kotlin project that provides a volume-button alternative for a phone with a broken power button.

## Shortcuts
- Double-press **Volume Down**: lock the screen.
- Double-press **Volume Up**: attempt to wake the display and show the lock screen.
- Single press: adjust media volume after a short double-tap detection delay.

## Open and build
1. Download or clone this repository.
2. Open the repository folder in Android Studio.
3. Use JDK 17 and install Android SDK Platform 35.
4. Sync Gradle, then use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Application ID: `com.pritambarman.volumetap`
Minimum Android: 6.0 (API 23)
Target Android API: 32

## One-time setup
1. Open VolumeTap and enable **lock permission** (Device Admin).
2. Enable **VolumeTap button gestures** in Android Accessibility settings.
3. Test screen lock before relying on the shortcut.
4. Permit background activity in Samsung battery settings if needed.

## Important limitation
Screen locking uses Android Device Admin's `lockNow()`. Waking depends on Samsung's input stack delivering volume-key events while the screen is asleep. Some firmware may suppress those events, so wake cannot be guaranteed on every Galaxy M32. The app does not bypass the lock screen or defeat PIN, password, pattern, or fingerprint security.

## Privacy
No Internet permission, analytics, account, or cloud service is used. The accessibility service requests hardware key filtering only and does not retrieve screen content.

## Build status
This repository contains source code, not a prebuilt APK. Build the APK locally in Android Studio.
