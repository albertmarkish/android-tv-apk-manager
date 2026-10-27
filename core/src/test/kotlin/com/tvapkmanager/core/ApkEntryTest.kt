package com.tvapkmanager.core

import org.junit.Test
import kotlin.test.assertEquals

class ApkEntryTest {
    private fun version(code: Long, pkg: String = "org.kodi", label: String = "Kodi") =
        AppVersion(packageName = pkg, label = label, versionName = "v$code", versionCode = code)

    private fun entry(app: AppVersion?, installed: AppVersion? = null, path: String = "/sdcard/${app?.label}.apk") =
        ApkEntry(path, path.substringAfterLast('/'), 1, 0, app, installed)

    @Test
    fun statusComparesVersionCodes() {
        assertEquals(InstallStatus.NOT_INSTALLED, InstallStatus.of(version(5), null))
        assertEquals(InstallStatus.UPDATE, InstallStatus.of(version(6), version(5)))
        assertEquals(InstallStatus.SAME_VERSION, InstallStatus.of(version(5), version(5)))
        assertEquals(InstallStatus.OLDER_THAN_INSTALLED, InstallStatus.of(version(4), version(5)))
        assertEquals(InstallStatus.UNREADABLE, InstallStatus.of(null, version(5)))
    }

    @Test
    fun installedVersionOfAnotherPackageDoesNotCount() {
        assertEquals(InstallStatus.NOT_INSTALLED, InstallStatus.of(version(5), version(9, pkg = "other")))
    }

    @Test
    fun unreadableFileIsLabelledByName() {
        assertEquals("broken.apk", entry(null, path = "/sdcard/broken.apk").label)
    }

    @Test
    fun sortsActionableFirstThenByName() {
        val sorted = listOf(
            entry(null, path = "/sdcard/broken.apk"),
            entry(version(5, pkg = "b", label = "beta"), installed = version(5, pkg = "b")),
            entry(version(1, pkg = "z", label = "Zed")),
            entry(version(2, pkg = "a", label = "alpha")),
            entry(version(9, pkg = "m", label = "Mid"), installed = version(3, pkg = "m")),
            entry(version(1, pkg = "o", label = "Old"), installed = version(3, pkg = "o")),
        ).sortedForDisplay()

        assertEquals(listOf("Mid", "alpha", "Zed", "Old", "beta", "broken.apk"), sorted.map { it.label })
    }

    @Test
    fun newestCopyOfTheSameAppComesFirst() {
        val sorted = listOf(
            entry(version(1), path = "/a.apk"),
            entry(version(3), path = "/b.apk"),
        ).sortedForDisplay()

        assertEquals(listOf("/b.apk", "/a.apk"), sorted.map { it.path })
    }

    @Test
    fun formatsSizes() {
        assertEquals("512 B", formatSize(512))
        assertEquals("1.5 KB", formatSize(1536))
        assertEquals("24.0 MB", formatSize(24L * 1024 * 1024))
        assertEquals("150 MB", formatSize(150L * 1024 * 1024))
        assertEquals("1.2 GB", formatSize(1288490189L))
    }

    @Test
    fun namesStorageLocationsPlainly() {
        assertEquals("Internal storage/Download/kodi.apk", friendlyLocation("/storage/emulated/0/Download/kodi.apk"))
        assertEquals("Internal storage/a.apk", friendlyLocation("/sdcard/a.apk"))
        assertEquals("USB drive 1A2B-3C4D/apps/a.apk", friendlyLocation("/storage/1A2B-3C4D/apps/a.apk"))
        assertEquals("/storage/emulated/10/a.apk", friendlyLocation("/storage/emulated/10/a.apk"))
    }
}
