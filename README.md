# APK Manager for Android TV

Finds every `.apk` file on the TV and installs it with one click of the remote. Made for the NVIDIA Shield, works on other Android TV and Google TV devices.

## What it does

- **Finds APKs on its own.** It searches internal storage (Download and every other folder) and every USB drive or SD card. Plugging in or removing a USB drive rescans by itself, and **Rescan** searches again.
- **Shows what's installed.** Each file shows the app's icon, name, version, size and location, with a status:
  - **Not installed**
  - **Update from 1.2**: an older version is installed, and this file updates it.
  - **Installed**: this exact version is already installed. Clicking reinstalls it.
  - **Newer 1.4 installed**: Android won't go back to an older version without uninstalling first.
  - **Can't read file**: damaged, still downloading, or a split APK bundle (`.apks`/`.xapk`, not supported yet).
- **Installs with Android's own installer.** Click a file, then confirm on the system screen. The list updates when the install finishes.

Files worth installing are listed first, then the ones already installed.

## First run on the Shield

1. Open **APK Manager** and choose **Allow access**. Turn on APK Manager in the **All files access** list and press Back. If that screen doesn't open, it's under **Settings > Apps > Special app access > All files access**. As a last resort, from a computer: `adb shell appops set --uid com.tvapkmanager.app MANAGE_EXTERNAL_STORAGE allow`.
2. The first time you install something, Android asks to allow APK Manager to install apps. Turn it on, press Back, and click the file again. On the Shield this switch lives in **Settings > Device Preferences > Security & restrictions > Unknown sources**.

## Project layout

- `core/` is plain Kotlin with no Android code: the folder scan (`ApkScanner`) and the installed/update/older rules (`InstallStatus`). It has unit tests.
- `app/` is the Android TV app: Jetpack Compose for TV. `ApkReader` reads each APK with the package manager, `ApkInstaller` and `InstallResultReceiver` run the install, and `StorageAccess` handles the storage permission.

The app targets the current Android API level, because Google Play Protect blocks sideloaded apps that target old ones. APK files aren't media, so on Android 11 and newer the app needs **All files access** (`MANAGE_EXTERNAL_STORAGE`) to see them in Downloads and on USB drives.

## Build

Needs JDK 17 and the Android SDK (Android Studio installs both).

```
./gradlew :core:test :app:assembleDebug
```

Every pull request is built by GitHub Actions, and the debug APK is attached to the run under **Artifacts**.

## Install on an NVIDIA Shield

1. On the Shield, open **Settings > Device Preferences > About** and click **Build** seven times to turn on developer options.
2. In **Settings > Device Preferences > Developer options**, turn on **Network debugging** and note the IP address shown.
3. On your computer: `adb connect <shield-ip>:5555`, accept the prompt on the TV, then `adb install -r app-debug.apk`.

After that, APK Manager can install everything else, including its own updates.
