# APK Manager for Android TV

Install apps on your Android TV from `.apk` files, using only the remote. APK Manager finds every APK on the TV and on plugged-in USB drives, shows which ones are already installed, and installs the rest with one click.

Made for the NVIDIA Shield. It also works on other Android TV and Google TV devices running Android 8.0 or newer.

## Features

- **Finds APKs on its own.** It searches internal storage (Download and every other folder) and every USB drive or SD card. Plugging in or removing a USB drive rescans by itself, and **Rescan** searches again.
- **Shows what's installed.** Each file shows the app's icon, name, version, size and location, with a status:
  - **Not installed**
  - **Update from 1.2**: an older version is installed, and this file updates it.
  - **Installed**: this exact version is already installed. Clicking reinstalls it.
  - **Newer 1.4 installed**: Android won't go back to an older version without uninstalling first.
  - **Can't read file**: damaged, still downloading, or a split APK bundle (`.apks`/`.xapk`, not supported yet).
- **Installs with Android's own installer.** Click a file, then confirm on the system screen. The list updates when the install finishes.
- **Keeps itself up to date.** When a new version of APK Manager is out, an **Update to x.y.z** button appears at the top. Click it, confirm, and reopen the app. **Check for updates** checks by hand.

Files worth installing are listed first, then the ones already installed.

## Download

Get the latest `apk-manager-x.y.z.apk` from the [Releases page](https://github.com/albertmarkish/android-tv-apk-manager/releases/latest). This direct link always points to the newest version:

```
https://github.com/albertmarkish/android-tv-apk-manager/releases/latest/download/apk-manager.apk
```

## Install

You only do this once. After that, APK Manager installs its own updates.

**With the Downloader app** (easiest, no computer needed)

1. Install **Downloader** by AFTVnews from the Google Play Store on the TV.
2. Allow Downloader to install apps: **Settings > Device Preferences > Security & restrictions > Unknown sources** (the path may differ on other TVs).
3. Open Downloader, enter the direct link above, and install the file when the download finishes.

**From a USB drive**

Copy the APK onto a USB drive, plug it into the TV, and open it with any file manager that can install apps.

**With adb from a computer**

1. On the TV, open **Settings > Device Preferences > About** and click **Build** seven times to turn on developer options.
2. In **Settings > Device Preferences > Developer options**, turn on **Network debugging** and note the IP address.
3. On the computer: `adb connect <tv-ip>:5555`, accept the prompt on the TV, then `adb install apk-manager.apk`.

## First run

1. Open **APK Manager** and choose **Allow access**. Turn on APK Manager in the **All files access** list and press Back. If that screen doesn't open, it's under **Settings > Apps > Special app access > All files access**. As a last resort, from a computer: `adb shell appops set --uid com.tvapkmanager.app MANAGE_EXTERNAL_STORAGE allow`.
2. The first time you install something, Android asks you to let APK Manager install apps. Turn it on, press Back, and click the file again. On the Shield this switch is in **Settings > Device Preferences > Security & restrictions > Unknown sources**.

## Permissions

- **All files access** (`MANAGE_EXTERNAL_STORAGE`): APK files aren't photos or videos, so on Android 11 and newer this is the only way to see them in Downloads and on USB drives. The app only reads files to find APKs.
- **Install unknown apps** (`REQUEST_INSTALL_PACKAGES`): to hand APKs to Android's installer.
- **See installed apps** (`QUERY_ALL_PACKAGES`): to compare each file with the version already installed.
- **Internet**: only to check this repository's releases for a newer APK Manager.

## Troubleshooting

- **"It clashes with the installed app"**: the installed copy was signed by someone else. For APK Manager itself, this happens with copies older than 0.3.0. Uninstall APK Manager, then install the latest release.
- **Blocked by Play Protect**: choose **Install anyway**. APK Manager targets the current Android version, so this shouldn't come up for the app itself.
- **No files found**: check that **All files access** is turned on, then press **Rescan**.

## Building from source

Needs JDK 17 and the Android SDK (Android Studio installs both).

```
./gradlew :core:test :app:assembleDebug
```

- `core/` is plain Kotlin with unit tests: the folder scan (`ApkScanner`), the install status rules (`InstallStatus`) and version comparison for updates.
- `app/` is the Android TV app, built with Jetpack Compose for TV.

Local builds are signed with your own debug key, so they can't update a copy installed from the Releases page. Uninstall that copy first.

Maintainers: see [docs/RELEASING.md](docs/RELEASING.md) for how releases are signed and published.
