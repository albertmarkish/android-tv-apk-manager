package com.tvapkmanager.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.tvapkmanager.app.ApkRow
import com.tvapkmanager.app.UiState
import com.tvapkmanager.core.ApkEntry
import com.tvapkmanager.core.InstallStatus
import com.tvapkmanager.core.formatSize
import com.tvapkmanager.core.friendlyLocation
import kotlinx.coroutines.delay

private val Background = Color(0xFF101418)
private val PrimaryText = Color(0xFFECEFF1)
private val SecondaryText = Color(0xFFB0BEC5)
private val MessageBackground = Color(0xFF1F2A33)

@Composable
fun ApkManagerScreen(
    state: UiState,
    onRescan: () -> Unit,
    onRowClick: (ApkRow) -> Unit,
    onAllowAccess: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(horizontal = 48.dp, vertical = 27.dp),
        ) {
            when (state.hasAccess) {
                null -> Unit
                false -> AccessScreen(onAllowAccess, onOpenAppSettings)
                true -> ApkList(state, onRescan, onRowClick)
            }
            state.message?.let { MessageBar(it) }
        }
    }
}

@Composable
private fun AccessScreen(onAllowAccess: () -> Unit, onOpenAppSettings: () -> Unit) {
    val allow = remember { FocusRequester() }
    LaunchedEffect(Unit) { requestFocusSoon(allow) }
    Column(modifier = Modifier.widthIn(max = 640.dp)) {
        Title()
        Spacer(Modifier.height(24.dp))
        Text(
            "To find APK files in Downloads and on USB drives, APK Manager needs All files access. " +
                "Choose Allow access, turn on APK Manager, then press Back.",
            color = PrimaryText,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onAllowAccess, modifier = Modifier.focusRequester(allow)) { Text("Allow access") }
            OutlinedButton(onClick = onOpenAppSettings) { Text("Open app settings") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "If nothing opens, go to Settings > Apps > Special app access > All files access.",
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Title() {
    Text("APK Manager", color = PrimaryText, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun ColumnScope.ApkList(
    state: UiState,
    onRescan: () -> Unit,
    onRowClick: (ApkRow) -> Unit,
) {
    val rescan = remember { FocusRequester() }
    val firstRow = remember { FocusRequester() }
    val hasRows = state.rows.isNotEmpty()
    LaunchedEffect(hasRows) { requestFocusSoon(if (hasRows) firstRow else rescan) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Title()
            Text(summary(state), color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onRescan, modifier = Modifier.focusRequester(rescan)) {
            Text(if (state.scanning) "Scanning…" else "Rescan")
        }
    }
    Spacer(Modifier.height(16.dp))

    if (!hasRows) {
        Text(
            if (state.scanning || !state.scannedOnce) {
                "Looking for APK files…"
            } else {
                "No APK files found. Copy .apk files to the TV's Download folder or plug in a USB drive, then press Rescan."
            },
            color = PrimaryText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(state.rows, key = { _, row -> row.entry.path }) { index, row ->
            ApkListItem(
                row = row,
                onClick = { onRowClick(row) },
                modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
            )
        }
    }
}

@Composable
private fun ApkListItem(row: ApkRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val entry = row.entry
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        headlineContent = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(details(entry), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            val icon = row.icon
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            } else {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color(0xFF37474F), RoundedCornerShape(8.dp)),
                )
            }
        },
        trailingContent = { StatusChip(entry) },
    )
}

@Composable
private fun StatusChip(entry: ApkEntry) {
    val installedVersion = entry.installed?.displayVersion
    val (text, color) = when (entry.status) {
        InstallStatus.NOT_INSTALLED -> "Not installed" to Color(0xFF2E7D32)
        InstallStatus.UPDATE -> "Update from $installedVersion" to Color(0xFF1565C0)
        InstallStatus.SAME_VERSION -> "Installed" to Color(0xFF455A64)
        InstallStatus.OLDER_THAN_INSTALLED -> "Newer $installedVersion installed" to Color(0xFF8D6E00)
        InstallStatus.UNREADABLE -> "Can't read file" to Color(0xFFB71C1C)
    }
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun MessageBar(text: String) {
    Text(
        text,
        color = PrimaryText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(MessageBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private fun details(entry: ApkEntry): String {
    val version = entry.app?.let { "Version ${it.displayVersion}" }
    return listOfNotNull(version, formatSize(entry.sizeBytes), friendlyLocation(entry.path)).joinToString("  ·  ")
}

private fun summary(state: UiState): String {
    if (!state.scannedOnce) return "Searching internal storage and USB drives"
    val total = state.rows.size
    val ready = state.rows.count { it.entry.status == InstallStatus.NOT_INSTALLED || it.entry.status == InstallStatus.UPDATE }
    val installed = state.rows.count {
        it.entry.status == InstallStatus.SAME_VERSION || it.entry.status == InstallStatus.OLDER_THAN_INSTALLED
    }
    val files = if (total == 1) "1 APK file" else "$total APK files"
    return "$files  ·  $ready to install or update  ·  $installed already installed"
}

/** Focus can only move once the target is laid out, which may be a frame or two after composition. */
private suspend fun requestFocusSoon(requester: FocusRequester) {
    repeat(10) {
        delay(50)
        if (runCatching { requester.requestFocus() }.isSuccess) return
    }
}
