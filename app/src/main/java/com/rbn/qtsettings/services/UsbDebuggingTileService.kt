package com.rbn.qtsettings.services

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.CountDownTimer
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.PermissionUtils

class UsbDebuggingTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager
    private var revertTimer: CountDownTimer? = null
    private val servicePrefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("usb_tile_service_state", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun savePreviousState(isUsbEnabled: Boolean) {
        servicePrefs.edit {
            putBoolean(PreferencesManager.KEY_USB_PREVIOUS_STATE_FOR_REVERT, isUsbEnabled)
        }
    }

    private fun getPreviousState(): Boolean? {
        return if (servicePrefs.contains(PreferencesManager.KEY_USB_PREVIOUS_STATE_FOR_REVERT)) {
            servicePrefs.getBoolean(PreferencesManager.KEY_USB_PREVIOUS_STATE_FOR_REVERT, false)
        } else {
            null
        }
    }

    private fun clearPreviousState() {
        servicePrefs.edit {
            remove(PreferencesManager.KEY_USB_PREVIOUS_STATE_FOR_REVERT)
        }
    }

    override fun onClick() {
        super.onClick()
        cancelRevertTimerWithMessage(getString(R.string.toast_revert_cancelled))

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
            updateTile()
            return
        }

        val currentUsbDebuggingState =
            Settings.Global.getInt(contentResolver, Constants.ADB_ENABLED, 0) == 1
        savePreviousState(currentUsbDebuggingState)

        val nextStatesToCycle = mutableListOf<Boolean>()
        if (prefsManager.isUsbToggleEnableEnabled()) nextStatesToCycle.add(true)
        if (prefsManager.isUsbToggleDisableEnabled()) nextStatesToCycle.add(false)

        if (nextStatesToCycle.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_states_enabled_usb, Toast.LENGTH_SHORT).show()
            clearPreviousState()
            return
        }

        var currentConfigIndex = nextStatesToCycle.indexOf(currentUsbDebuggingState)
        if (currentConfigIndex == -1) {
            currentConfigIndex = -1
        }

        val nextConfigIndex = (currentConfigIndex + 1) % nextStatesToCycle.size
        val nextStateToSet = nextStatesToCycle[nextConfigIndex]

        try {
            Settings.Global.putInt(
                contentResolver,
                Constants.ADB_ENABLED,
                if (nextStateToSet) 1 else 0
            )

            if (prefsManager.isUsbAutoRevertEnabled()) {
                val delaySeconds = prefsManager.getUsbAutoRevertDelaySeconds()
                if (delaySeconds > 0) {
                    startRevertTimer(delaySeconds)
                    val prevEnabledState = getPreviousState() ?: currentUsbDebuggingState
                    val readablePrevState =
                        if (prevEnabledState) getString(R.string.on_state) else getString(R.string.off_state)
                    val toastMsg =
                        getString(R.string.toast_reverting_usb_to, readablePrevState, delaySeconds)
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
                } else {
                    clearPreviousState()
                }
            } else {
                clearPreviousState()
            }

        } catch (e: Exception) {
            Log.e("UsbDebuggingTile", "Error setting USB Debug: ${e.message}", e)
            Toast.makeText(this, R.string.toast_error_saving_settings, Toast.LENGTH_SHORT).show()
            clearPreviousState()
        }
        updateTile()
    }

    private fun startRevertTimer(delaySeconds: Int) {
        revertTimer?.cancel()
        revertTimer = object : CountDownTimer(delaySeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                qsTile?.let { tile ->
                    getPreviousState()?.let { prevUsbState ->
                        val readablePrevState =
                            if (prevUsbState) getString(R.string.on_state) else getString(R.string.off_state)
                        tile.subtitle = getString(
                            R.string.tile_subtitle_reverting_in_seconds,
                            readablePrevState,
                            millisUntilFinished / 1000
                        )
                        tile.updateTile()
                    }
                }
            }

            override fun onFinish() {
                getPreviousState()?.let { prevUsbState ->
                    try {
                        if (PermissionUtils.isDeveloperOptionsEnabled(applicationContext)) {
                            Settings.Global.putInt(
                                contentResolver,
                                Constants.ADB_ENABLED,
                                if (prevUsbState) 1 else 0
                            )
                            Log.i(
                                "UsbDebuggingTile",
                                "Auto-reverted USB Debug to ${if (prevUsbState) "ON" else "OFF"}"
                            )
                            val revertedStateString =
                                if (prevUsbState) getString(R.string.on_state) else getString(R.string.off_state)
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.usb_state_reverted_to, revertedStateString),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.w(
                                "UsbDebuggingTile",
                                "Developer options disabled, cannot auto-revert USB debugging."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("UsbDebuggingTile", "Error auto-reverting USB: ${e.message}", e)
                    } finally {
                        clearPreviousState()
                        revertTimer = null
                        updateTile()
                    }
                }
            }
        }.start()
    }

    private fun cancelRevertTimerWithMessage(message: String?) {
        if (revertTimer != null) {
            revertTimer?.cancel()
            revertTimer = null
            clearPreviousState()
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            Log.i("UsbDebuggingTile", "Revert timer cancelled.")
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        if (revertTimer == null || getPreviousState() == null) {
            tile.subtitle = ""
        }

        if (!PermissionUtils.isDeveloperOptionsEnabled(this)) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.usb_dev_options_off)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_usb_off)
            tile.updateTile()
            if (revertTimer != null) {
                cancelRevertTimerWithMessage(getString(R.string.toast_developer_options_disabled_revert_cancelled))
            }
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

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertTimer?.cancel()
        revertTimer = null
    }
}