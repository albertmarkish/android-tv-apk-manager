package com.tvapkmanager.app

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tvapkmanager.core.ApkScanner
import com.tvapkmanager.core.InstallStatus
import com.tvapkmanager.core.Release
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
    /** A newer APK Manager on GitHub, once the check at launch finds one. */
    val update: Release? = null,
    /** Download progress in percent while an update is being fetched. */
    val updateProgress: Int? = null,
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
    private val updater = SelfUpdater(activity)
    private var scanJob: Job? = null
    private var updateJob: Job? = null

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
            showMessage("Open Settings > Apps > Special app access > All files access and turn on APK Manager.")
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

    /** Quietly looks for a newer APK Manager. Only a manual check reports that there's nothing new. */
    fun checkForUpdate(manual: Boolean = false) {
        if (updateJob?.isActive == true) return
        if (manual) showMessage("Checking for a newer APK Manager…")
        updateJob = scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { updater.findUpdate() } }
            val release = result.getOrNull()
            state = state.copy(update = release)
            when {
                release != null -> showMessage(
                    "APK Manager ${release.version} is available (you have ${updater.currentVersion}). Choose Update to install it.",
                )
                !manual -> Unit
                result.isSuccess -> showMessage("APK Manager ${updater.currentVersion} is the latest version.")
                else -> showMessage("Couldn't check for updates: ${result.exceptionOrNull()?.message ?: "no connection"}")
            }
        }
    }

    fun installUpdate() {
        val release = state.update ?: return checkForUpdate(manual = true)
        if (updateJob?.isActive == true) return
        if (!ensureCanInstall(retry = "choose Update again")) return
        state = state.copy(updateProgress = 0)
        showMessage("Downloading APK Manager ${release.version}…")
        updateJob = scope.launch {
            val error = withContext(Dispatchers.IO) {
                runCatching {
                    val file = updater.download(release) { percent ->
                        scope.launch { state = state.copy(updateProgress = percent) }
                    }
                    installer.install(file, "$APP_LABEL ${release.version}")
                }.exceptionOrNull()
            }
            state = state.copy(updateProgress = null)
            if (error != null) {
                showMessage("Couldn't update APK Manager: ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    /** Opens the "Unknown sources" switch when it's still off. Returns true when installing can go ahead. */
    private fun ensureCanInstall(retry: String): Boolean {
        if (installer.canInstall()) return true
        val opened = installer.openInstallPermissionSettings()
        showMessage(
            if (opened) {
                "Turn on APK Manager in this list, press Back, then $retry."
            } else {
                "Allow APK Manager in Settings > Device Preferences > Security & restrictions > Unknown sources, then $retry."
            },
        )
        return false
    }

    private fun install(path: String, label: String) {
        if (!ensureCanInstall(retry = "pick $label again")) return
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

    private companion object {
        const val APP_LABEL = "APK Manager"
    }
}
