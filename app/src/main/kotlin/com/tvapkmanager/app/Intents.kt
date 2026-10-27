package com.tvapkmanager.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

/** Android TV builds leave out many settings screens, so each request tries a few in order. */
fun Activity.startFirstAvailable(vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            // Try the next one.
        } catch (e: SecurityException) {
            // Try the next one.
        }
    }
    return false
}

fun Activity.packageUri(): Uri = Uri.parse("package:$packageName")
