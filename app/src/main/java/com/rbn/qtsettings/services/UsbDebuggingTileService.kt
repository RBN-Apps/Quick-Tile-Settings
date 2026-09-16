package com.rbn.qtsettings.services

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.rbn.qtsettings.QuickTileSettingsApplication
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.domain.UsbAutoRevert
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Android lifecycle, lock screen and tile rendering; USB policy lives in the feature. */
class UsbDebuggingTileService : TileService() {
    private lateinit var store: UsbSettingsStore
    private lateinit var actions: UsbActions
    private lateinit var autoRevert: UsbAutoRevert
    private var subtitleScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as QuickTileSettingsApplication).container
        store = AppStores.getInstance(applicationContext).usb
        // Share the process-wide action and revert graph so a pending revert survives this
        // service and so direct actions from the app cancel it.
        actions = container.usbActions
        autoRevert = container.usbAutoRevert
    }

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { subtitleScope = it }
        scope.launch {
            autoRevert.remainingSeconds.collectLatest { remaining ->
                if (remaining == null) updateTile() else renderSubtitle(remaining)
            }
        }
        updateTile()
    }

    /** The countdown now lives outside this service, so the tile only renders its ticks. */
    private fun renderSubtitle(remaining: Int) {
        val tile = qsTile ?: return
        val previous = autoRevert.previous ?: return
        tile.subtitle = getString(R.string.tile_subtitle_reverting_in_seconds, stateLabel(previous.usb), remaining)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        cancelTimer(R.string.toast_revert_cancelled)
        if (!actions.hasPermission()) {
            show(R.string.toast_permission_not_granted_adb)
            return
        }
        if (store.read().requireUnlock && isLocked) unlockAndRun { toggle() } else toggle()
    }

    private fun toggle() {
        val settings = store.read()
        val (result, previous, generation) = actions.cycle(settings)
        if (result == UsbActionResult.Success && previous != null &&
            settings.enableAutoRevert && settings.autoRevertDelaySeconds > 0) {
            val seconds = settings.autoRevertDelaySeconds
            // Capture the application context only: this callback can outlive the service.
            val context = applicationContext
            autoRevert.schedule(previous, generation, seconds, store::read) { restored, restoredTo ->
                announceRevert(context, restored, restoredTo)
            }
            Toast.makeText(this, getString(R.string.toast_reverting_usb_to,
                stateLabel(previous.usb), seconds), Toast.LENGTH_LONG).show()
        } else if (result != UsbActionResult.Success) showError(result)
        updateTile()
    }

    private fun showError(result: UsbActionResult) = show(errorMessage(result))
    private fun show(resource: Int) = Toast.makeText(this, resource, Toast.LENGTH_SHORT).show()

    private companion object {
        /** Static so a revert landing after the service is gone keeps nothing alive. */
        fun announceRevert(context: Context, restored: UsbActionResult?, previous: UsbSystemState) {
            val message = when {
                restored == UsbActionResult.Success -> context.getString(R.string.usb_state_reverted_to,
                    context.getString(if (previous.usb) R.string.on_state else R.string.off_state))
                restored != null -> context.getString(errorMessage(restored))
                else -> return
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun errorMessage(result: UsbActionResult) = when (result) {
            UsbActionResult.PermissionMissing -> R.string.toast_permission_not_granted_adb
            UsbActionResult.DeveloperOptionsDisabled -> R.string.toast_developer_options_disabled
            UsbActionResult.NoCycleStates -> R.string.toast_no_states_enabled_usb
            else -> R.string.toast_error_saving_settings
        }
    }
    private fun stateLabel(enabled: Boolean) = getString(if (enabled) R.string.on_state else R.string.off_state)

    private fun cancelTimer(message: Int? = null) {
        val wasPending = autoRevert.cancel()
        if (wasPending && message != null) show(message)
        if (wasPending) updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (autoRevert.remainingSeconds.value == null) tile.subtitle = ""
        val settings = store.read()
        val state = actions.read()
        if (!settings.alsoHideDevOptions && !state.developerOptions) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.usb_dev_options_off)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_usb_off)
            tile.updateTile()
            cancelTimer(R.string.toast_developer_options_disabled_revert_cancelled)
            return
        }
        val active = state.usb && (!settings.alsoHideDevOptions || state.developerOptions)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(if (active) R.string.usb_state_on else R.string.usb_state_off)
        tile.icon = Icon.createWithResource(this, if (active) R.drawable.ic_usb_on else R.drawable.ic_usb_off)
        tile.updateTile()
    }

    override fun onStopListening() {
        subtitleScope?.cancel()
        subtitleScope = null
        super.onStopListening()
    }

    // The pending revert deliberately outlives this service now; only the rendering stops.
    override fun onDestroy() {
        subtitleScope?.cancel()
        subtitleScope = null
        super.onDestroy()
    }
}
