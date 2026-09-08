package com.rbn.qtsettings.app.navigation

import android.content.ComponentName
import android.content.Intent
import androidx.core.content.IntentCompat

/** Accept the platform ComponentName and the string form used by our Quick Actions. */
fun Intent.resolveTileArea(): AppArea? {
    val component = IntentCompat.getParcelableExtra(this, Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
    val text = component?.className ?: getStringExtra(Intent.EXTRA_COMPONENT_NAME) ?: return null
    return when {
        text.contains("PrivateDnsTileService") -> AppArea.Dns
        text.contains("UsbDebuggingTileService") -> AppArea.Usb
        else -> null
    }
}
