package com.tvapkmanager.core

/** What an APK file says about itself, and what the TV has installed for the same package. */
data class ApkEntry(
    val path: String,
    val fileName: String,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    /** Null when the file couldn't be read as an APK (damaged, partial download, or a split bundle). */
    val app: AppVersion?,
    val installed: AppVersion?,
) {
    val label: String get() = app?.label ?: fileName

    val status: InstallStatus get() = InstallStatus.of(app, installed)
}

data class AppVersion(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
) {
    /** "1.4.2", or the version code when the APK doesn't name its version. */
    val displayVersion: String get() = versionName?.takeIf { it.isNotBlank() } ?: versionCode.toString()
}

enum class InstallStatus {
    /** The file is newer than what's installed. */
    UPDATE,
    NOT_INSTALLED,
    /** A newer version is installed. Android refuses the downgrade unless the app is uninstalled first. */
    OLDER_THAN_INSTALLED,
    SAME_VERSION,
    UNREADABLE;

    companion object {
        fun of(app: AppVersion?, installed: AppVersion?): InstallStatus = when {
            app == null -> UNREADABLE
            installed == null || installed.packageName != app.packageName -> NOT_INSTALLED
            app.versionCode > installed.versionCode -> UPDATE
            app.versionCode < installed.versionCode -> OLDER_THAN_INSTALLED
            else -> SAME_VERSION
        }
    }
}

/** Things worth installing first, then the rest, each group alphabetical. */
fun List<ApkEntry>.sortedForDisplay(): List<ApkEntry> =
    sortedWith(
        compareBy<ApkEntry> { it.status.ordinal }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            // Several copies of one app: newest version first.
            .thenByDescending { it.app?.versionCode ?: 0L }
            .thenBy { it.path },
    )

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value >= 100) "%.0f %s".format(value, units[unit]) else "%.1f %s".format(value, units[unit])
}

/** "Internal storage/Download/kodi.apk" instead of "/storage/emulated/0/Download/kodi.apk". */
fun friendlyLocation(path: String): String {
    INTERNAL_PREFIXES.firstOrNull { path == it || path.startsWith("$it/") }?.let {
        return "Internal storage" + path.removePrefix(it)
    }
    val volume = VOLUME_PATH.matchEntire(path) ?: return path
    return "USB drive ${volume.groupValues[1]}" + volume.groupValues[2]
}

private val INTERNAL_PREFIXES = listOf("/storage/emulated/0", "/sdcard", "/storage/self/primary")
private val VOLUME_PATH = Regex("/storage/([0-9A-Fa-f]{4}-[0-9A-Fa-f]{4})(/.*)?")
