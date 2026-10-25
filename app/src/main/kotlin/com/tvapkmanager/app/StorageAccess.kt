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
 * Reading every folder needs one of two things. The app targets API 29 with
 * requestLegacyExternalStorage, so on the Shield (Android 11) the normal storage prompt is enough.
 * If a future Android ignores legacy storage, it falls back to "All files access".
 */
object StorageAccess {

    fun hasAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) return true
        if (needsAllFilesAccess()) return false
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageLegacy()

    /** Returns false when this TV has no screen for granting access. */
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

    /** For when the prompt was turned down with "Don't ask again" and no longer appears. */
    fun openAppSettings(activity: Activity): Boolean =
        activity.startFirstAvailable(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, activity.packageUri()))
}
