package com.rbn.qtsettings.services

import android.graphics.drawable.Icon
import android.os.CountDownTimer
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.rbn.qtsettings.R
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.domain.UsbAutoRevert
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway

/** Android lifecycle, lock screen and tile rendering; USB policy lives in the feature. */
class UsbDebuggingTileService : TileService() {
    private lateinit var store: UsbSettingsStore
    private lateinit var actions: UsbActions
    private lateinit var autoRevert: UsbAutoRevert
    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        store = AppStores.getInstance(applicationContext).usb
        actions = UsbActions(AndroidUsbSystemGateway(this))
        autoRevert = UsbAutoRevert(actions)
    }

    override fun onStartListening() { super.onStartListening(); updateTile() }

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
            autoRevert.schedule(previous, generation)
            val seconds = settings.autoRevertDelaySeconds
            timer = object : CountDownTimer(seconds * 1000L, 1000) {
                override fun onTick(remaining: Long) {
                    if (!autoRevert.isCurrent()) { cancelTimer(); return }
                    qsTile?.let {
                        it.subtitle = getString(R.string.tile_subtitle_reverting_in_seconds,
                            stateLabel(previous.usb), remaining / 1000)
                        it.updateTile()
                    }
                }
                override fun onFinish() {
                    val restored = autoRevert.finish(store.read())
                    timer = null
                    if (restored == UsbActionResult.Success) {
                        Toast.makeText(this@UsbDebuggingTileService,
                            getString(R.string.usb_state_reverted_to, stateLabel(previous.usb)), Toast.LENGTH_SHORT).show()
                    } else if (restored != null) showError(restored)
                    updateTile()
                }
            }.start()
            Toast.makeText(this, getString(R.string.toast_reverting_usb_to,
                stateLabel(previous.usb), seconds), Toast.LENGTH_LONG).show()
        } else if (result != UsbActionResult.Success) showError(result)
        updateTile()
    }

    private fun showError(result: UsbActionResult) = show(when (result) {
        UsbActionResult.PermissionMissing -> R.string.toast_permission_not_granted_adb
        UsbActionResult.DeveloperOptionsDisabled -> R.string.toast_developer_options_disabled
        UsbActionResult.NoCycleStates -> R.string.toast_no_states_enabled_usb
        else -> R.string.toast_error_saving_settings
    })
    private fun show(resource: Int) = Toast.makeText(this, resource, Toast.LENGTH_SHORT).show()
    private fun stateLabel(enabled: Boolean) = getString(if (enabled) R.string.on_state else R.string.off_state)

    private fun cancelTimer(message: Int? = null) {
        val wasPending = timer != null
        timer?.cancel()
        timer = null
        autoRevert.cancel()
        if (wasPending && message != null) show(message)
        if (wasPending) updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (timer == null) tile.subtitle = ""
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

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        autoRevert.cancel()
        super.onDestroy()
    }
}
