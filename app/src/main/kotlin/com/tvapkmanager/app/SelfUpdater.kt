package com.tvapkmanager.app

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.tvapkmanager.core.Release
import com.tvapkmanager.core.isNewerVersion
import com.tvapkmanager.core.versionFromTag
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Finds newer builds of APK Manager on its GitHub releases page and downloads them.
 * CI publishes one release per version, each signed with the same key, so Android accepts it as an update.
 */
class SelfUpdater(private val context: Context) {

    val currentVersion: String = BuildConfig.VERSION_NAME

    /** Blocking. Returns the latest release when it's newer than this build, otherwise null. */
    fun findUpdate(): Release? {
        val json = open(LATEST_RELEASE_URL, accept = "application/vnd.github+json").use { connection ->
            connection.inputStream.bufferedReader().readText()
        }
        val release = parse(JSONObject(json)) ?: return null
        return release.takeIf { isNewerVersion(it.version, currentVersion) }
    }

    /** Blocking. Downloads the release APK into the cache and checks it really is a newer APK Manager. */
    fun download(release: Release, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "apk-manager-${release.version}.apk")
        open(release.apkUrl, accept = "application/octet-stream").use { connection ->
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        val percent = if (total > 0) (done * 100 / total).toInt().coerceAtMost(100) else 0
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
        }
        verify(file)
        return file
    }

    private fun verify(file: File) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, 0)
            ?: throw IOException("The download isn't a complete APK.")
        if (archive.packageName != context.packageName) throw IOException("The download is a different app.")
        val installed = pm.getPackageInfo(context.packageName, 0)
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            throw IOException("The download isn't newer than this version.")
        }
    }

    private fun parse(json: JSONObject): Release? {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (!asset.optString("name").endsWith(".apk")) continue
            return Release(
                version = versionFromTag(json.optString("tag_name")),
                apkUrl = asset.getString("browser_download_url"),
                apkSizeBytes = asset.optLong("size"),
            )
        }
        return null
    }

    private fun open(url: String, accept: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("User-Agent", "APK-Manager/$currentVersion")
        if (connection.responseCode !in 200..299) {
            val code = connection.responseCode
            connection.disconnect()
            throw IOException(if (code == 404) "No release has been published yet." else "GitHub answered $code.")
        }
        return connection
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private companion object {
        val LATEST_RELEASE_URL = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"
    }
}
