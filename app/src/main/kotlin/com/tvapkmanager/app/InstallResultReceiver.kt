package com.tvapkmanager.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface InstallEvent {
    val label: String

    data class WaitingForConfirmation(override val label: String) : InstallEvent
    data class Installed(override val label: String) : InstallEvent
    data class Failed(override val label: String, val reason: String) : InstallEvent
}

/** Install progress from the receiver to whatever screen is showing. */
object InstallEvents {
    private val flow = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<InstallEvent> = flow

    fun emit(event: InstallEvent) {
        flow.tryEmit(event)
    }
}

/** Android reports each install session's progress here. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "The app"
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    InstallEvents.emit(InstallEvent.WaitingForConfirmation(label))
                } else {
                    InstallEvents.emit(InstallEvent.Failed(label, "Android didn't show the install screen."))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvents.emit(InstallEvent.Installed(label))
            else -> InstallEvents.emit(
                InstallEvent.Failed(label, describe(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))),
            )
        }
    }

    private fun describe(status: Int, systemMessage: String?): String = when (status) {
        PackageInstaller.STATUS_FAILURE_ABORTED -> "The install was cancelled."
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Android blocked the install."
        PackageInstaller.STATUS_FAILURE_CONFLICT ->
            "It clashes with the installed app, usually because it was signed by someone else. Uninstall the current app first."
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "It isn't made for this TV."
        PackageInstaller.STATUS_FAILURE_INVALID -> "The file isn't a complete APK."
        PackageInstaller.STATUS_FAILURE_STORAGE -> "There isn't enough free space on the TV."
        else -> systemMessage?.takeIf { it.isNotBlank() } ?: "Android didn't say why."
    }

    companion object {
        private const val ACTION = "com.tvapkmanager.app.INSTALL_RESULT"
        private const val EXTRA_LABEL = "label"

        fun intentSender(context: Context, sessionId: Int, label: String): IntentSender {
            val intent = Intent(context, InstallResultReceiver::class.java)
                .setAction(ACTION)
                .putExtra(EXTRA_LABEL, label)
            // The installer adds the status extras, so the intent has to stay mutable.
            val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
                .intentSender
        }
    }
}
