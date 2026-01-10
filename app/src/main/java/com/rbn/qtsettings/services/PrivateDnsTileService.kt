package com.rbn.qtsettings.services

import android.content.SharedPreferences
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils

class PrivateDnsTileService : TileService() {

    private lateinit var prefsManager: PreferencesManager
    private var revertTimer: CountDownTimer? = null
    private val servicePrefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("dns_tile_service_state", MODE_PRIVATE)
    }

    private var isVpnConnected = false
    private var vpnMonitorTimer: CountDownTimer? = null
    private var currentNetworkType: String = NETWORK_TYPE_NONE
    private var networkTypeMonitorTimer: CountDownTimer? = null

    private var dnsSettingsObserver: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        initializeVpnState()
        startVpnMonitoring()
        initializeNetworkTypeState()
        startNetworkTypeMonitoring()
        startObservingDnsSettings()
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

    private fun initializeVpnState() {
        if (!prefsManager.isVpnDetectionEnabled()) {
            clearVpnPreviousState()
            isVpnConnected = false
            return
        }

        val detectionMode = prefsManager.getVpnDetectionMode()
        if (detectionMode != TILE_ONLY_DETECTION) {
            isVpnConnected = VpnDetectionUtils.isVpnActive(this)
            return
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            return
        }

        isVpnConnected = VpnDetectionUtils.isVpnActive(this)

        if (isVpnConnected) {
            val existingVpnState = getVpnPreviousState()
            if (existingVpnState == null) {
                val currentMode = VpnDetectionUtils.getCurrentPrivateDnsMode(this)
                if (currentMode == DNS_MODE_OFF) {
                    Log.w(
                        "PrivateDnsTile",
                        "VPN is active with DNS off, but no saved state found. Cannot restore original DNS settings."
                    )
                } else {
                    onVpnConnected()
                }
            }
        } else {
            val existingVpnState = getVpnPreviousState()
            if (existingVpnState != null) {
                onVpnDisconnected()
            }
        }
    }

    private fun onVpnConnected() {
        try {
            val currentMode = VpnDetectionUtils.getCurrentPrivateDnsMode(this)
            val currentHostname = VpnDetectionUtils.getCurrentPrivateDnsHostname(this)

            saveVpnPreviousState(currentMode, currentHostname)

            if (currentMode != DNS_MODE_OFF) {
                VpnDetectionUtils.setPrivateDnsOff(this)
                Log.i("PrivateDnsTile", "VPN detected: Set Private DNS to off")
            }
        } catch (e: Exception) {
            Log.e("PrivateDnsTile", "Error handling VPN connection", e)
        }
    }

    private fun onVpnDisconnected() {
        try {
            val (previousMode, previousHostname) = getVpnPreviousState() ?: return

            VpnDetectionUtils.restorePrivateDns(this, previousMode, previousHostname)
            clearVpnPreviousState()

            Log.i("PrivateDnsTile", "VPN disconnected: Restored Private DNS to $previousMode")
        } catch (e: Exception) {
            Log.e("PrivateDnsTile", "Error handling VPN disconnection", e)
        }
    }

    private fun saveVpnPreviousState(mode: String, hostname: String?) {
        val sharedVpnPrefs = applicationContext.getSharedPreferences(
            "vpn_detection_shared_state",
            MODE_PRIVATE
        )
        sharedVpnPrefs.edit {
            putString("vpn_previous_dns_mode", mode)
            putString("vpn_previous_dns_hostname", hostname)
        }
    }

    private fun getVpnPreviousState(): Pair<String, String?>? {
        val sharedVpnPrefs = applicationContext.getSharedPreferences(
            "vpn_detection_shared_state",
            MODE_PRIVATE
        )
        val mode = sharedVpnPrefs.getString("vpn_previous_dns_mode", null)
        return mode?.let {
            Pair(it, sharedVpnPrefs.getString("vpn_previous_dns_hostname", null))
        }
    }

    private fun clearVpnPreviousState() {
        val sharedVpnPrefs = applicationContext.getSharedPreferences(
            "vpn_detection_shared_state",
            MODE_PRIVATE
        )
        sharedVpnPrefs.edit {
            remove("vpn_previous_dns_mode")
            remove("vpn_previous_dns_hostname")
        }
    }

    private fun startVpnMonitoring() {
        if (!prefsManager.isVpnDetectionEnabled() || prefsManager.getVpnDetectionMode() != TILE_ONLY_DETECTION) {
            return
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            return
        }

        vpnMonitorTimer?.cancel()
        vpnMonitorTimer = object : CountDownTimer(Long.MAX_VALUE, 2000) { // Check every 2 seconds
            override fun onTick(millisUntilFinished: Long) {
                val currentVpnState = VpnDetectionUtils.isVpnActive(this@PrivateDnsTileService)

                if (currentVpnState != isVpnConnected) {
                    if (currentVpnState) {
                        onVpnConnected()
                    } else {
                        onVpnDisconnected()
                    }
                    isVpnConnected = currentVpnState
                    updateTile()
                }
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun stopVpnMonitoring() {
        vpnMonitorTimer?.cancel()
        vpnMonitorTimer = null
    }

    private fun startObservingDnsSettings() {
        val isBackgroundMode = prefsManager.isVpnDetectionEnabled() &&
                prefsManager.getVpnDetectionMode() == BACKGROUND_DETECTION

        if (isBackgroundMode) {
            stopObservingDnsSettings()

            dnsSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    updateTile()
                }
            }

            val dnsModUri = Settings.Global.getUriFor(PRIVATE_DNS_MODE)
            val dnsSpecifierUri = Settings.Global.getUriFor(PRIVATE_DNS_SPECIFIER)

            contentResolver.registerContentObserver(dnsModUri, false, dnsSettingsObserver!!)
            contentResolver.registerContentObserver(dnsSpecifierUri, false, dnsSettingsObserver!!)
        }
    }

    private fun stopObservingDnsSettings() {
        dnsSettingsObserver?.let { observer ->
            contentResolver.unregisterContentObserver(observer)
            dnsSettingsObserver = null
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

        val currentMode = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
            ?: DNS_MODE_OFF
        val currentHost =
            Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)

        savePreviousState(currentMode, currentHost)

        val nextStates = mutableListOf<Pair<String, String?>>()
        if (prefsManager.isDnsToggleOffEnabled()) nextStates.add(Pair(DNS_MODE_OFF, null))
        if (prefsManager.isDnsToggleAutoEnabled()) nextStates.add(
            Pair(
                DNS_MODE_AUTO,
                null
            )
        )
        val hostnamesToCycle = prefsManager.getDnsHostnamesSelectedForCycle()
        hostnamesToCycle.forEach { entry ->
            nextStates.add(Pair(DNS_MODE_ON, entry.hostname))
        }

        if (nextStates.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_states_enabled_dns, Toast.LENGTH_SHORT).show()
            clearPreviousState()
            return
        }

        val currentIndex =
            if (currentMode == DNS_MODE_ON) {
                nextStates.indexOfFirst { it.first == DNS_MODE_ON && it.second == currentHost }
            } else {
                nextStates.indexOfFirst { it.first == currentMode }
            }
        val nextIndex = (currentIndex + 1) % nextStates.size
        val (nextMode, nextHostToSet) = nextStates[nextIndex]

        try {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, nextMode)
            if (nextMode == DNS_MODE_ON) {
                if (nextHostToSet.isNullOrBlank()) {
                    Toast.makeText(this, R.string.toast_hostname_required, Toast.LENGTH_LONG).show()
                    getPreviousState()?.let { (prevMode, prevHost) ->
                        Settings.Global.putString(
                            contentResolver,
                            PRIVATE_DNS_MODE,
                            prevMode
                        )
                        if (prevMode == DNS_MODE_ON && prevHost != null) {
                            Settings.Global.putString(
                                contentResolver,
                                PRIVATE_DNS_SPECIFIER,
                                prevHost
                            )
                        }
                    } ?: Settings.Global.putString(
                        contentResolver,
                        PRIVATE_DNS_MODE,
                        DNS_MODE_OFF
                    )
                    clearPreviousState()
                    updateTile()
                    return
                }
                Settings.Global.putString(
                    contentResolver,
                    PRIVATE_DNS_SPECIFIER,
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
                            PRIVATE_DNS_MODE,
                            prevMode
                        )
                        if (prevMode == DNS_MODE_ON && prevHost != null) {
                            Settings.Global.putString(
                                contentResolver,
                                PRIVATE_DNS_SPECIFIER,
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
            DNS_MODE_OFF -> getString(R.string.off_state)
            DNS_MODE_AUTO -> getString(R.string.auto_state)
            DNS_MODE_ON -> {
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

    private fun getDnsIcon(hostname: String?): Int {
        return when (hostname) {
            "dns.adguard.com" -> R.drawable.ic_dns_on_adguard
            "one.one.one.one" -> R.drawable.ic_dns_on_cloudflare
            "dns.quad9.net" -> R.drawable.ic_dns_on_quad9_security
            else -> R.drawable.ic_dns_on
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return

        if (revertTimer == null || getPreviousState() == null) {
            tile.subtitle = ""
        }

        val dnsMode = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)

        when (dnsMode) {
            DNS_MODE_OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.dns_state_off)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_off)
            }

            DNS_MODE_AUTO -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.dns_state_auto)
                tile.icon = Icon.createWithResource(this, R.drawable.ic_dns_auto)
            }

            DNS_MODE_ON -> {
                tile.state = Tile.STATE_ACTIVE
                val currentActualHostname =
                    Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)

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
                tile.icon = Icon.createWithResource(this, getDnsIcon(currentActualHostname))
                if (currentActualHostname.isNullOrBlank() && PermissionUtils.hasWriteSecureSettingsPermission(
                        this
                    )
                ) {
                    getPreviousState()?.let { (prevMode, prevHost) ->
                        if (prevMode != DNS_MODE_ON || !prevHost.isNullOrBlank()) {
                            Settings.Global.putString(
                                contentResolver,
                                PRIVATE_DNS_MODE,
                                prevMode
                            )
                            if (prevHost != null && prevMode == DNS_MODE_ON) {
                                Settings.Global.putString(
                                    contentResolver,
                                    PRIVATE_DNS_SPECIFIER,
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
                            PRIVATE_DNS_MODE,
                            DNS_MODE_OFF
                        )
                        updateTile()
                        return
                    } else if (prefsManager.isDnsToggleAutoEnabled()) {
                        Settings.Global.putString(
                            contentResolver,
                            PRIVATE_DNS_MODE,
                            DNS_MODE_AUTO
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
        stopVpnMonitoring()
        stopNetworkTypeMonitoring()
        stopObservingDnsSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertTimer?.cancel()
        revertTimer = null
        stopVpnMonitoring()
        stopNetworkTypeMonitoring()
        stopObservingDnsSettings()
    }

    private fun initializeNetworkTypeState() {
        if (!prefsManager.isNetworkTypeDetectionEnabled()) {
            currentNetworkType = NETWORK_TYPE_NONE
            return
        }

        val detectionMode = prefsManager.getNetworkTypeDetectionMode()
        if (detectionMode != TILE_ONLY_DETECTION) {
            return
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            return
        }

        currentNetworkType = NetworkTypeDetectionUtils.getCurrentNetworkType(this)
    }

    private fun startNetworkTypeMonitoring() {
        if (!prefsManager.isNetworkTypeDetectionEnabled() ||
            prefsManager.getNetworkTypeDetectionMode() != TILE_ONLY_DETECTION
        ) {
            return
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            return
        }

        networkTypeMonitorTimer?.cancel()
        networkTypeMonitorTimer = object : CountDownTimer(Long.MAX_VALUE, 2000) { // Check every 2 seconds
            override fun onTick(millisUntilFinished: Long) {
                val detectedNetworkType = NetworkTypeDetectionUtils.getCurrentNetworkType(
                    this@PrivateDnsTileService
                )

                if (detectedNetworkType != currentNetworkType) {
                    handleNetworkTypeChange(detectedNetworkType)
                    currentNetworkType = detectedNetworkType
                    updateTile()
                }
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun stopNetworkTypeMonitoring() {
        networkTypeMonitorTimer?.cancel()
        networkTypeMonitorTimer = null
    }

    private fun handleNetworkTypeChange(newNetworkType: String) {
        // Don't apply DNS changes if VPN is active (VPN takes priority)
        if (isVpnConnected) {
            Log.d("PrivateDnsTile", "VPN is active, skipping network type DNS change")
            return
        }

        Log.i("PrivateDnsTile", "Network type changed to: $newNetworkType")

        when (newNetworkType) {
            NETWORK_TYPE_WIFI -> {
                val dnsState = prefsManager.getDnsStateOnWifi()
                val dnsHostname = prefsManager.getDnsHostnameOnWifi()
                NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
                    this,
                    NETWORK_TYPE_WIFI,
                    dnsState,
                    dnsHostname
                )
            }

            NETWORK_TYPE_MOBILE -> {
                val dnsState = prefsManager.getDnsStateOnMobile()
                val dnsHostname = prefsManager.getDnsHostnameOnMobile()
                NetworkTypeDetectionUtils.setPrivateDnsForNetworkType(
                    this,
                    NETWORK_TYPE_MOBILE,
                    dnsState,
                    dnsHostname
                )
            }

            NETWORK_TYPE_NONE -> {
                Log.d("PrivateDnsTile", "No active network")
            }
        }
    }
}