# Releasing

GitHub Actions builds every push and pull request. A push to `main` with a new version publishes a GitHub release, which is where the app looks for updates.

## Publishing a release

1. Raise `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Merge to `main`.

CI creates the `vX.Y.Z` release with the APK attached twice: as `apk-manager-X.Y.Z.apk` and as `apk-manager.apk`, so `releases/latest/download/apk-manager.apk` always gives the newest version. A merge that keeps the same version publishes nothing.

The app reads `releases/latest` without signing in, so the repository (or wherever `UPDATE_REPO` in `app/build.gradle.kts` points) has to stay public.

## Signing

Android only installs an update when it's signed with the same key as the installed app, so CI signs every build with one release key kept in two repository secrets:

- `RELEASE_KEYSTORE_BASE64`: the PKCS12 keystore (alias `apkmanager`), base64 encoded.
- `RELEASE_KEYSTORE_PASSWORD`: its password, used for both the store and the key.

Without the secrets (for example on a fork) builds fall back to the debug key, and the release step fails rather than publish an APK that can't update existing installs.

Keep a backup of the keystore somewhere safe. If it's lost, no future build can update installed copies, and everyone has to uninstall and reinstall.
