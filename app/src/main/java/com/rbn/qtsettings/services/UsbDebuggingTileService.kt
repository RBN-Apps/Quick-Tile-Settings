package com.rbn.qtsettings.services

import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.PermissionUtils

class UsbDebuggingTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
            Log.w("UsbDebuggingTile", "WRITE_SECURE_SETTINGS permission not granted.")
            return
        }

        if (!PermissionUtils.isDeveloperOptionsEnabled(this)) {
            Toast.makeText(this, R.string.toast_developer_options_disabled, Toast.LENGTH_LONG)
                .show()
            Log.w("UsbDebuggingTile", "Developer options are disabled.")
            return
        }

        val currentUsbDebuggingState =
            Settings.Global.getInt(contentResolver, Constants.ADB_ENABLED, 0) == 1

        val nextStates = mutableListOf<Boolean>()
        if (prefsManager.isUsbToggleEnableEnabled()) nextStates.add(true)
        if (prefsManager.isUsbToggleDisableEnabled()) nextStates.add(false)

        if (nextStates.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_states_enabled_usb, Toast.LENGTH_SHORT).show()
            return
        }

        var currentConfigIndex = nextStates.indexOf(currentUsbDebuggingState)
        if (currentConfigIndex == -1) {
            currentConfigIndex = -1
        }

        val nextConfigIndex = (currentConfigIndex + 1) % nextStates.size
        val nextStateToSet = nextStates[nextConfigIndex]

        try {
            Settings.Global.putInt(
                contentResolver,
                Constants.ADB_ENABLED,
                if (nextStateToSet) 1 else 0
            )
        } catch (e: SecurityException) {
            Log.e("UsbDebuggingTile", "SecurityException: Failed to write ADB_ENABLED setting.", e)
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Log.e("UsbDebuggingTile", "Failed to write ADB_ENABLED setting.", e)
            Toast.makeText(this, R.string.toast_error_saving_settings, Toast.LENGTH_SHORT).show()
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        if (!PermissionUtils.isDeveloperOptionsEnabled(this)) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.usb_dev_options_off)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_usb_off)
            tile.updateTile()
            return
        }

        val adbEnabled = Settings.Global.getInt(contentResolver, Constants.ADB_ENABLED, 0) == 1

        if (adbEnabled) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.usb_state_on)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_usb_on)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.usb_state_off)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_usb_off)
        }
        tile.updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}