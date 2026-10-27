package com.tvapkmanager.core

import java.io.File
import java.io.IOException

/**
 * Finds .apk files under a set of storage roots, such as the TV's internal storage and any
 * USB drives. Plain java.io so it can be tested on a normal JVM.
 */
class ApkScanner(private val maxDepth: Int = DEFAULT_MAX_DEPTH) {

    fun scan(roots: List<File>): List<File> {
        val found = LinkedHashMap<String, File>()
        val visitedDirs = HashSet<String>()

        for (root in distinctRoots(roots)) {
            val pending = ArrayDeque<Pair<File, Int>>()
            pending.addLast(root to 0)
            while (pending.isNotEmpty()) {
                val (dir, depth) = pending.removeFirst()
                // Symlinks and bind mounts can point back up the tree; each real folder is walked once.
                if (!visitedDirs.add(canonicalPath(dir))) continue
                val children = dir.listFiles() ?: continue
                for (child in children.sortedBy { it.name.lowercase() }) {
                    if (child.isDirectory) {
                        if (depth < maxDepth && !isSkipped(root, child)) pending.addLast(child to depth + 1)
                    } else if (isApk(child)) {
                        found.putIfAbsent(canonicalPath(child), child)
                    }
                }
            }
        }
        return found.values.toList()
    }

    /** Drops roots that don't exist, duplicates, and roots that sit inside another root. */
    internal fun distinctRoots(roots: List<File>): List<File> {
        val byPath = roots.filter { it.isDirectory }
            .associateBy { canonicalPath(it) }
            .toSortedMap()
        val kept = mutableListOf<Pair<String, File>>()
        for ((path, file) in byPath) {
            if (kept.none { (keptPath, _) -> path.startsWith(keptPath.trimEnd('/') + "/") }) kept += path to file
        }
        return kept.map { it.second }
    }

    private fun isSkipped(root: File, dir: File): Boolean {
        if (dir.name.startsWith(".")) return true
        // Other apps' private folders: unreadable on recent Android and slow to walk on older ones.
        val parent = dir.parentFile ?: return false
        return parent.name == "Android" && parent.parentFile == root && dir.name in PRIVATE_APP_DIRS
    }

    companion object {
        const val DEFAULT_MAX_DEPTH = 12
        private val PRIVATE_APP_DIRS = setOf("data", "obb", "media")

        fun isApk(file: File): Boolean = file.isFile && file.name.endsWith(".apk", ignoreCase = true)

        private fun canonicalPath(file: File): String =
            try {
                file.canonicalPath
            } catch (e: IOException) {
                file.absolutePath
            }
    }
}
