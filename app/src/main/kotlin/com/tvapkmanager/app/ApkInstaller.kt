package com.tvapkmanager.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import java.io.File

/** Hands APK files to Android's own installer, which asks the viewer to confirm on screen. */
class ApkInstaller(private val activity: Activity) {

    fun canInstall(): Boolean = activity.packageManager.canRequestPackageInstalls()

    /** Opens the "Unknown sources" switch for this app. Returns false when the TV has no such screen. */
    fun openInstallPermissionSettings(): Boolean =
        activity.startFirstAvailable(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, activity.packageUri()),
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )

    /** Blocking: copies the file into an install session. Call it off the main thread. */
    fun install(file: File, label: String) {
        val installer = activity.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setSize(file.length())
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("base.apk", 0, file.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                session.commit(InstallResultReceiver.intentSender(activity, sessionId, label))
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }
}
