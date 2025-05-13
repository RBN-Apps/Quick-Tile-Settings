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

class PrivateDnsTileService : TileService() {

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
            Log.w("PrivateDnsTile", "WRITE_SECURE_SETTINGS permission not granted.")
            return
        }

        val currentMode = Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_MODE)
        val currentHost =
            Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_SPECIFIER)
                ?: prefsManager.getDnsHostname()

        val nextStates = mutableListOf<Pair<String, String?>>()
        if (prefsManager.isDnsToggleOffEnabled()) nextStates.add(Pair(Constants.DNS_MODE_OFF, null))
        if (prefsManager.isDnsToggleAutoEnabled()) nextStates.add(
            Pair(
                Constants.DNS_MODE_AUTO,
                null
            )
        )
        if (prefsManager.isDnsToggleOnEnabled()) nextStates.add(
            Pair(
                Constants.DNS_MODE_ON,
                prefsManager.getDnsHostname()
            )
        )

        if (nextStates.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_states_enabled_dns, Toast.LENGTH_SHORT).show()
            return
        }
        if (nextStates.size == 1 && prefsManager.isDnsToggleOnEnabled() && prefsManager.getDnsHostname()
                .isBlank()
        ) {
            Toast.makeText(this, R.string.toast_hostname_required, Toast.LENGTH_LONG).show()
            return
        }


        var currentIndex =
            nextStates.indexOfFirst { it.first == currentMode && (it.first != Constants.DNS_MODE_ON || it.second == currentHost) }
        if (currentIndex == -1) {
            currentIndex = -1
        }

        val nextIndex = (currentIndex + 1) % nextStates.size
        val (nextMode, nextHost) = nextStates[nextIndex]

        try {
            Settings.Global.putString(contentResolver, Constants.PRIVATE_DNS_MODE, nextMode)
            if (nextMode == Constants.DNS_MODE_ON) {
                if (nextHost.isNullOrBlank()) {
                    Toast.makeText(this, R.string.toast_hostname_required, Toast.LENGTH_LONG).show()
                    if (prefsManager.isDnsToggleOffEnabled()) {
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            Constants.DNS_MODE_OFF
                        )
                    } else if (prefsManager.isDnsToggleAutoEnabled()) {
                        Settings.Global.putString(
                            contentResolver,
                            Constants.PRIVATE_DNS_MODE,
                            Constants.DNS_MODE_AUTO
                        )
                    }
                } else {
                    Settings.Global.putString(
                        contentResolver,
                        Constants.PRIVATE_DNS_SPECIFIER,
                        nextHost
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("PrivateDnsTile", "SecurityException: Failed to write Private DNS settings.", e)
            Toast.makeText(this, R.string.toast_permission_not_granted_adb, Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Log.e("PrivateDnsTile", "Failed to write Private DNS settings.", e)
            Toast.makeText(this, R.string.toast_error_saving_settings, Toast.LENGTH_SHORT).show()
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
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
                val hostname =
                    Settings.Global.getString(contentResolver, Constants.PRIVATE_DNS_SPECIFIER)
                        ?: prefsManager.getDnsHostname()
                tile.label =
                    if (hostname.length > 15) hostname.take(12) + "..." else hostname
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_on)
                if (hostname.isBlank() && PermissionUtils.hasWriteSecureSettingsPermission(this)) {
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

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}