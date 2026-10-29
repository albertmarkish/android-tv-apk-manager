package com.tvapkmanager.core

/** A published build of APK Manager itself, as listed on its GitHub releases page. */
data class Release(
    /** "0.3.0", without the "v" the tag starts with. */
    val version: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
)

/**
 * Compares dotted versions like "0.10.2" number by number, so 0.10 is newer than 0.9.
 * Anything after the numbers ("-beta") is ignored.
 */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = versionParts(candidate) ?: return false
    val b = versionParts(current) ?: return true
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff > 0
    }
    return false
}

/** "v0.3.0" and "0.3.0" both become "0.3.0". */
fun versionFromTag(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

private fun versionParts(version: String): List<Int>? {
    val numbers = VERSION_NUMBERS.find(versionFromTag(version))?.value ?: return null
    return numbers.split('.').map { it.toIntOrNull() ?: return null }
}

private val VERSION_NUMBERS = Regex("^\\d+(\\.\\d+)*")
