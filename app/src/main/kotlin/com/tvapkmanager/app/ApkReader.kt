package com.tvapkmanager.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.tvapkmanager.core.ApkEntry
import com.tvapkmanager.core.AppVersion
import java.io.File

data class ApkRow(val entry: ApkEntry, val icon: ImageBitmap?)

/** Reads each APK's name, version and icon, and looks up the installed version of the same app. */
class ApkReader(context: Context) {
    private val pm = context.packageManager
    private val iconPx = (ICON_DP * context.resources.displayMetrics.density).toInt()

    fun read(file: File): ApkRow {
        val archive = runCatching { pm.getPackageArchiveInfo(file.path, 0) }.getOrNull()
        // Resources inside an APK that isn't installed only load once its own path is set.
        val appInfo = archive?.applicationInfo?.apply {
            sourceDir = file.path
            publicSourceDir = file.path
        }
        val app = if (archive != null && appInfo != null) {
            AppVersion(
                packageName = archive.packageName,
                label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(archive.packageName),
                versionName = archive.versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(archive),
            )
        } else {
            null
        }
        val icon = appInfo?.let { info ->
            runCatching { info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap() }.getOrNull()
        }
        val entry = ApkEntry(
            path = file.path,
            fileName = file.name,
            sizeBytes = file.length(),
            modifiedMillis = file.lastModified(),
            app = app,
            installed = installed(app?.packageName),
        )
        return ApkRow(entry, icon)
    }

    fun installed(packageName: String?): AppVersion? {
        if (packageName == null) return null
        val info = try {
            pm.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return AppVersion(
            packageName = packageName,
            label = info.applicationInfo?.loadLabel(pm)?.toString() ?: packageName,
            versionName = info.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
        )
    }

    private companion object {
        const val ICON_DP = 48
    }
}
