package com.rbn.qtsettings.platform.tiles

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService

/**
 * Android 13's own "add this tile" dialog. The system owns the decision and the placement; the app
 * only asks. Older releases have no such request, so the panel is edited by hand there.
 */
class TilePanelRequests(private val activity: Activity) {

    /** Reports whether the tile is in the panel afterwards, including when it already was. */
    fun request(tile: OnboardingTile, onResult: (Boolean) -> Unit) {
        if (!AVAILABLE) return onResult(false)
        requestFromApi33(tile, onResult)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestFromApi33(tile: OnboardingTile, onResult: (Boolean) -> Unit) {
        val manager = activity.getSystemService(StatusBarManager::class.java) ?: return onResult(false)
        val service = when (tile) {
            OnboardingTile.Dns -> PrivateDnsTileService::class.java
            OnboardingTile.Usb -> UsbDebuggingTileService::class.java
        }
        val label = activity.getString(when (tile) {
            OnboardingTile.Dns -> R.string.tile_label_private_dns
            OnboardingTile.Usb -> R.string.tile_label_usb_debugging
        })
        val icon = when (tile) {
            OnboardingTile.Dns -> R.drawable.ic_dns_auto
            OnboardingTile.Usb -> R.drawable.ic_usb_off
        }
        try {
            manager.requestAddTileService(
                ComponentName(activity, service), label, Icon.createWithResource(activity, icon),
                activity.mainExecutor
            ) { result ->
                onResult(result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED)
            }
        } catch (_: IllegalStateException) {
            // The request is rejected while the app is not in the foreground.
            onResult(false)
        }
    }

    companion object {
        val AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
