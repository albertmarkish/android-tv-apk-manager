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

- **Updates itself.** At launch it checks this repo's GitHub releases. When a newer APK Manager is out, an **Update to x.y.z** button appears: click it, confirm on the system screen, and reopen the app. **Check for updates** does the same check by hand.

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

Every pull request is built by GitHub Actions, and the APKs are attached to the run under **Artifacts**.

## Releases and signing

Android only installs an update when it's signed with the same key as the installed app, so CI signs every build with one release key kept in two repository secrets:

- `RELEASE_KEYSTORE_BASE64`: the PKCS12 keystore (alias `apkmanager`), base64 encoded.
- `RELEASE_KEYSTORE_PASSWORD`: its password, used for both the store and the key.

Keep a backup of the keystore somewhere safe. If it's lost, no future build can update the installed app, and it has to be uninstalled and installed again.

To publish a release, raise `versionName` and `versionCode` in `app/build.gradle.kts` and merge to `main`. CI creates the `vX.Y.Z` release with `apk-manager-X.Y.Z.apk` attached, which is what the app downloads. Merges that keep the same version don't publish anything.

The app reads releases without signing in to GitHub, so the repository (or wherever `UPDATE_REPO` points) has to be public.

## Install on an NVIDIA Shield

1. On the Shield, open **Settings > Device Preferences > About** and click **Build** seven times to turn on developer options.
2. In **Settings > Device Preferences > Developer options**, turn on **Network debugging** and note the IP address shown.
3. On your computer: `adb connect <shield-ip>:5555`, accept the prompt on the TV, then `adb install -r app-debug.apk`.

After that, APK Manager can install everything else, including its own updates.

Builds from before 0.3.0 were signed with a throwaway debug key. To move to the release-signed builds, uninstall APK Manager once and install the APK from the latest release. Updates install in place from then on.
