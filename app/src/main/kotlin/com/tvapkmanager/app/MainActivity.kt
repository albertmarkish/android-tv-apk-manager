package com.tvapkmanager.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.tvapkmanager.app.ui.ApkManagerScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val scope = MainScope()
    private lateinit var controller: ApkManagerController

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { controller.onResume() }

    /** Plugging in or removing a USB drive rescans on its own. */
    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            controller.scan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ApkManagerController(this, scope, permissionLauncher)
        if (savedInstanceState == null) controller.checkForUpdate()

        setContent {
            LaunchedEffect(Unit) {
                InstallEvents.events.collect(controller::onInstallEvent)
            }
            ApkManagerScreen(
                state = controller.state,
                onRescan = controller::scan,
                onRowClick = controller::onRowClicked,
                onUpdate = controller::installUpdate,
                onAllowAccess = controller::requestAccess,
                onOpenAppSettings = controller::openAppSettings,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(this, mediaReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onStop() {
        unregisterReceiver(mediaReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
