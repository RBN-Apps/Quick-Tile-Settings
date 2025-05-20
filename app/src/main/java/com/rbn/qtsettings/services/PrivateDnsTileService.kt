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

class PrivateDnsTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager
    private var revertTimer: CountDownTimer? = null
    private val servicePrefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("dns_tile_service_state", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun savePreviousState(mode: String, hostname: String?) {
        servicePrefs.edit {
            putString(PreferencesManager.KEY_DNS_PREVIOUS_MODE_FOR_REVERT, mode)
                .putString(PreferencesManager.KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT, hostname)
        }
    }

    private fun getPreviousState(): Pair<String, String?>? {
        val mode = servicePrefs.getString(PreferencesManager.KEY_DNS_PREVIOUS_MODE_FOR_REVERT, null)
        return mode?.let {
            Pair(
                it,
                servicePrefs.getString(
                    PreferencesManager.KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT,
                    null
                )
            )
        }
    }

    private fun clearPreviousState() {
        servicePrefs.edit {
            remove(PreferencesManager.KEY_DNS_PREVIOUS_MODE_FOR_REVERT)
                .remove(PreferencesManager.KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT)
        }
    }


    override fun onClick() {
        super.onClick()
        cancelRevertTimerWithMessage(getString(R.string.toast_revert_cancelled))

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
            Log.w("PrivateDnsTile", "WRITE_SECURE_SETTINGS permission not granted.")
            return
        }

        val currentMode = Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_MODE)
            ?: Constants.DNS_MODE_OFF
        val currentHost =
            Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_SPECIFIER)

        savePreviousState(currentMode, currentHost)

        val nextStates = mutableListOf<Pair<String, String?>>()
        if (prefsManager.isDnsToggleOffEnabled()) nextStates.add(Pair(Constants.DNS_MODE_OFF, null))
        if (prefsManager.isDnsToggleAutoEnabled()) nextStates.add(
            Pair(
                Constants.DNS_MODE_AUTO,
                null
            )
        )
        val hostnamesToCycle = prefsManager.getDnsHostnamesSelectedForCycle()
        hostnamesToCycle.forEach { entry ->
            nextStates.add(Pair(Constants.DNS_MODE_ON, entry.hostname))
        }

        if (nextStates.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_states_enabled_dns, Toast.LENGTH_SHORT).show()
            clearPreviousState()
            return
        }

        val currentIndex =
            if (currentMode == Constants.DNS_MODE_ON) {
                nextStates.indexOfFirst { it.first == Constants.DNS_MODE_ON && it.second == currentHost }
            } else {
                nextStates.indexOfFirst { it.first == currentMode }
            }
        val nextIndex = (currentIndex + 1) % nextStates.size
        val (nextMode, nextHostToSet) = nextStates[nextIndex]

        try {
            Settings.Global.putString(contentResolver, Constants.PRIVATE_DNS_MODE, nextMode)
            if (nextMode == Constants.DNS_MODE_ON) {
                if (nextHostToSet.isNullOrBlank()) {
                    Toast.makeText(this, R.string.toast_hostname_required, Toast.LENGTH_LONG).show()
                    getPreviousState()?.let { (prevMode, prevHost) ->
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            prevMode
                        )
                        if (prevMode == Constants.DNS_MODE_ON && prevHost != null) {
                            Settings.Global.putString(
                                contentResolver,
                                Constants.PRIVATE_DNS_SPECIFIER,
                                prevHost
                            )
                        }
                    } ?: Settings.Global.putString(
                        contentResolver,
                        Constants.PRIVATE_DNS_MODE,
                        Constants.DNS_MODE_OFF
                    )
                    clearPreviousState()
                    updateTile()
                    return
                }
                Settings.Global.putString(
                    contentResolver,
                    Constants.PRIVATE_DNS_SPECIFIER,
                    nextHostToSet
                )
            }

            if (prefsManager.isDnsAutoRevertEnabled()) {
                val delaySeconds = prefsManager.getDnsAutoRevertDelaySeconds()
                if (delaySeconds > 0) {
                    startRevertTimer(delaySeconds)
                    val prevModeForToast = getPreviousState()?.first ?: currentMode
                    val prevHostForToast = getPreviousState()?.second
                    val toastMsg = getString(
                        R.string.toast_reverting_dns_to,
                        getReadableDnsMode(prevModeForToast, prevHostForToast),
                        delaySeconds
                    )
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
                } else {
                    clearPreviousState()
                }
            } else {
                clearPreviousState()
            }

        } catch (e: Exception) {
            Log.e("PrivateDnsTile", "Error setting DNS: ${e.message}", e)
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
                    getPreviousState()?.let { (prevMode, prevHost) ->
                        val readablePrevMode = getReadableDnsMode(prevMode, prevHost)
                        tile.subtitle = getString(
                            R.string.tile_subtitle_reverting_in_seconds,
                            readablePrevMode,
                            millisUntilFinished / 1000
                        )
                        tile.updateTile()
                    }
                }
            }

            override fun onFinish() {
                getPreviousState()?.let { (prevMode, prevHost) ->
                    try {
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            prevMode
                        )
                        if (prevMode == Constants.DNS_MODE_ON && prevHost != null) {
                            Settings.Global.putString(
                                contentResolver,
                                Constants.PRIVATE_DNS_SPECIFIER,
                                prevHost
                            )
                        }
                        Log.i("PrivateDnsTile", "Auto-reverted DNS to $prevMode")
                        Toast.makeText(
                            applicationContext,
                            getString(
                                R.string.dns_state_reverted_to,
                                getReadableDnsMode(prevMode, prevHost)
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e("PrivateDnsTile", "Error auto-reverting DNS: ${e.message}", e)
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
            Log.i("PrivateDnsTile", "Revert timer cancelled.")
            updateTile()
        }
    }


    private fun getReadableDnsMode(mode: String, hostname: String? = null): String {
        return when (mode) {
            Constants.DNS_MODE_OFF -> getString(R.string.off_state)
            Constants.DNS_MODE_AUTO -> getString(R.string.auto_state)
            Constants.DNS_MODE_ON -> {
                if (hostname != null) {
                    val entry =
                        prefsManager.getAllDnsHostnamesBlocking().find { it.hostname == hostname }
                    entry?.name ?: hostname
                } else {
                    getString(R.string.on_state)
                }
            }

            else -> mode
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        if (revertTimer == null || getPreviousState() == null) {
            tile.subtitle = ""
        }

        val dnsMode = Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_MODE)

        when (dnsMode) {
            Constants.DNS_MODE_OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.dns_state_off)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_off)
            }

            Constants.DNS_MODE_AUTO -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.dns_state_auto)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_auto)
            }

            Constants.DNS_MODE_ON -> {
                tile.state = Tile.STATE_ACTIVE
                val currentActualHostname =
                    Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_SPECIFIER)

                if (currentActualHostname.isNullOrBlank() && PermissionUtils.hasWriteSecureSettingsPermission(
                        this
                    )
                ) {
                    tile.label =
                        getString(R.string.dns_state_on_with_host)
                } else if (!currentActualHostname.isNullOrBlank()) {
                    val entry = prefsManager.getAllDnsHostnamesBlocking()
                        .find { it.hostname == currentActualHostname }
                    val displayName = entry?.name ?: currentActualHostname
                    tile.label =
                        if (displayName.length > 15) displayName.take(12) + "..." else displayName
                } else {
                    tile.label = getString(R.string.dns_state_on_with_host)
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_on)
                if (currentActualHostname.isNullOrBlank() && PermissionUtils.hasWriteSecureSettingsPermission(
                        this
                    )
                ) {
                    getPreviousState()?.let { (prevMode, prevHost) ->
                        if (prevMode != Constants.DNS_MODE_ON || !prevHost.isNullOrBlank()) {
                            Settings.Global.putString(
                                contentResolver,
                                Constants.PRIVATE_DNS_MODE,
                                prevMode
                            )
                            if (prevMode == Constants.DNS_MODE_ON && prevHost != null) {
                                Settings.Global.putString(
                                    contentResolver,
                                    Constants.PRIVATE_DNS_SPECIFIER,
                                    prevHost
                                )
                            }
                            clearPreviousState()
                            updateTile()
                            return
                        }
                    }
                    if (prefsManager.isDnsToggleOffEnabled()) {
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            Constants.DNS_MODE_OFF
                        )
                        updateTile()
                        return
                    } else if (prefsManager.isDnsToggleAutoEnabled()) {
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            Constants.DNS_MODE_AUTO
                        )
                        updateTile()
                        return
                    }
                }
            }

            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.dns_state_unknown)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_off)
            }
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