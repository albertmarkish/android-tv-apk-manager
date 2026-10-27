package com.tvapkmanager.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

/**
 * APK files aren't media, so on Android 11 and newer only "All files access" can see them in
 * Downloads and on USB drives. Android 10 and older use the normal storage prompt, with
 * requestLegacyExternalStorage covering Android 10.
 */
object StorageAccess {

    fun hasAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) return true
        if (needsAllFilesAccess()) return false
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageLegacy()

    /** Returns false when this TV has no screen for granting access, so the caller shows ADB steps. */
    fun request(activity: Activity, permissionLauncher: ActivityResultLauncher<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && needsAllFilesAccess()) {
            return activity.startFirstAvailable(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, activity.packageUri()),
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            )
        }
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        return true
    }

    /** For when the direct screen didn't open, or the prompt was turned down with "Don't ask again". */
    fun openAppSettings(activity: Activity): Boolean {
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, activity.packageUri())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && needsAllFilesAccess()) {
            return activity.startFirstAvailable(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), appDetails)
        }
        return activity.startFirstAvailable(appDetails)
    }
}
