package com.tvapkmanager.app

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tvapkmanager.core.ApkScanner
import com.tvapkmanager.core.InstallStatus
import com.tvapkmanager.core.sortedForDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class UiState(
    /** Null until the first check, so nothing flashes on screen. */
    val hasAccess: Boolean? = null,
    val scanning: Boolean = false,
    val scannedOnce: Boolean = false,
    val rows: List<ApkRow> = emptyList(),
    val message: String? = null,
)

class ApkManagerController(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val permissionLauncher: ActivityResultLauncher<String>,
) {
    var state by mutableStateOf(UiState())
        private set

    private val reader = ApkReader(activity)
    private val installer = ApkInstaller(activity)
    private var scanJob: Job? = null

    /** Called whenever the app comes back to the front, including after settings and install screens. */
    fun onResume() {
        val access = StorageAccess.hasAccess(activity)
        state = state.copy(hasAccess = access)
        if (!access) return
        if (!state.scannedOnce) scan() else refreshInstalled()
    }

    fun scan() {
        if (state.hasAccess != true) return
        scanJob?.cancel()
        state = state.copy(scanning = true)
        scanJob = scope.launch {
            val rows = withContext(Dispatchers.IO) {
                ApkScanner().scan(StorageRoots.find(activity)).map(reader::read)
            }
            state = state.copy(scanning = false, scannedOnce = true, rows = sorted(rows))
        }
    }

    /** Re-checks installed versions without walking storage again. */
    private fun refreshInstalled() {
        if (state.scanning) return
        val current = state.rows
        scope.launch {
            val rows = withContext(Dispatchers.IO) {
                current.map { row ->
                    row.copy(entry = row.entry.copy(installed = reader.installed(row.entry.app?.packageName)))
                }
            }
            if (!state.scanning) state = state.copy(rows = sorted(rows))
        }
    }

    fun requestAccess() {
        if (!StorageAccess.request(activity, permissionLauncher)) {
            showMessage(
                "This TV has no screen for file access. From a computer, run: " +
                    "adb shell appops set --uid ${activity.packageName} MANAGE_EXTERNAL_STORAGE allow",
            )
        }
    }

    fun openAppSettings() {
        if (!StorageAccess.openAppSettings(activity)) {
            showMessage("Open Settings > Apps > APK Manager > Permissions and turn on Storage.")
        }
    }

    fun onRowClicked(row: ApkRow) {
        val entry = row.entry
        when (entry.status) {
            InstallStatus.UNREADABLE -> showMessage(
                "Android can't read ${entry.fileName}. It may be damaged, still downloading, or a split APK bundle.",
            )
            InstallStatus.OLDER_THAN_INSTALLED -> showMessage(
                "${entry.label} ${entry.installed?.displayVersion} is installed, which is newer than this file " +
                    "(${entry.app?.displayVersion}). Uninstall it first to go back to this version.",
            )
            else -> install(entry.path, entry.label)
        }
    }

    private fun install(path: String, label: String) {
        if (!installer.canInstall()) {
            val opened = installer.openInstallPermissionSettings()
            showMessage(
                if (opened) {
                    "Turn on APK Manager in this list, press Back, then pick $label again."
                } else {
                    "Allow APK Manager in Settings > Device Preferences > Security & restrictions > Unknown sources, then pick $label again."
                },
            )
            return
        }
        showMessage("Getting $label ready…")
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching { installer.install(File(path), label) }.exceptionOrNull()
            }
            if (error != null) showMessage("Couldn't start installing $label: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    fun onInstallEvent(event: InstallEvent) {
        when (event) {
            is InstallEvent.WaitingForConfirmation -> showMessage("Confirm installing ${event.label} on screen.")
            is InstallEvent.Installed -> {
                showMessage("${event.label} is installed.")
                refreshInstalled()
            }
            is InstallEvent.Failed -> showMessage("${event.label} wasn't installed. ${event.reason}")
        }
    }

    private fun showMessage(text: String) {
        state = state.copy(message = text)
    }

    private fun sorted(rows: List<ApkRow>): List<ApkRow> {
        val byPath = rows.associateBy { it.entry.path }
        return rows.map { it.entry }.sortedForDisplay().mapNotNull { byPath[it.path] }
    }
}
