package com.tvapkmanager.core

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

class ApkScannerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(path: String): File =
        File(tmp.root, path).apply {
            parentFile.mkdirs()
            writeText("x")
        }

    private fun names(files: List<File>) = files.map { it.relativeTo(tmp.root).path }.sorted()

    @Test
    fun findsApksInNestedFoldersOnly() {
        file("Download/kodi.apk")
        file("Download/notes.txt")
        file("Movies/apps/old/Player.APK")
        file("Download/apk-not-really.apk.part")

        val found = ApkScanner().scan(listOf(tmp.root))

        assertEquals(listOf("Download/kodi.apk", "Movies/apps/old/Player.APK"), names(found))
    }

    @Test
    fun skipsHiddenFoldersAndOtherAppsPrivateData() {
        file(".thumbnails/a.apk")
        file("Android/data/com.example/cache/b.apk")
        file("Android/obb/com.example/c.apk")
        file("Android/apps/d.apk")
        file("Backups/Android/data/e.apk")

        val found = ApkScanner().scan(listOf(tmp.root))

        assertEquals(listOf("Android/apps/d.apk", "Backups/Android/data/e.apk"), names(found))
    }

    @Test
    fun respectsMaxDepth() {
        file("a/top.apk")
        file("a/b/c/deep.apk")

        val found = ApkScanner(maxDepth = 2).scan(listOf(tmp.root))

        assertEquals(listOf("a/top.apk"), names(found))
    }

    @Test
    fun overlappingAndMissingRootsAreWalkedOnce() {
        file("Download/kodi.apk")
        val roots = listOf(tmp.root, File(tmp.root, "Download"), tmp.root, File(tmp.root, "missing"))

        assertEquals(listOf(tmp.root.canonicalFile), ApkScanner().distinctRoots(roots).map { it.canonicalFile })
        assertEquals(listOf("Download/kodi.apk"), names(ApkScanner().scan(roots)))
    }

    @Test
    fun siblingRootWithSharedPrefixIsKept() {
        val usb = File(tmp.root, "usb").apply { mkdirs() }
        val usb2 = File(tmp.root, "usb2").apply { mkdirs() }

        assertEquals(2, ApkScanner().distinctRoots(listOf(usb, usb2)).size)
    }

    @Test
    fun symlinkLoopsDoNotHang() {
        file("Download/kodi.apk")
        Files.createSymbolicLink(File(tmp.root, "Download/loop").toPath(), tmp.root.toPath())

        assertEquals(listOf("Download/kodi.apk"), names(ApkScanner().scan(listOf(tmp.root))))
    }
}
