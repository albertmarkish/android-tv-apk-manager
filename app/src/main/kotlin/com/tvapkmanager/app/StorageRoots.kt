package com.tvapkmanager.app

import android.content.Context
import android.os.Environment
import java.io.File

/** Internal storage plus every mounted USB drive or SD card. */
object StorageRoots {

    fun find(context: Context): List<File> {
        val roots = mutableListOf(Environment.getExternalStorageDirectory())
        // Each mounted volume reports this app's folder as <volume>/Android/data/<package>/files.
        context.getExternalFilesDirs(null).filterNotNull().mapNotNullTo(roots) { volumeRoot(it) }
        File("/storage").listFiles()?.filterTo(roots) {
            it.isDirectory && it.name != "self" && it.name != "emulated"
        }
        return roots
    }

    private fun volumeRoot(appFilesDir: File): File? {
        val path = appFilesDir.absolutePath
        val index = path.indexOf("/Android/data/")
        return if (index > 0) File(path.substring(0, index)) else null
    }
}
