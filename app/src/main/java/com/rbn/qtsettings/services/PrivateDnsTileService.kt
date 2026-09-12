package com.rbn.qtsettings.services

import android.content.SharedPreferences
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
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
import com.rbn.qtsettings.QuickTileSettingsApplication
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.dns.data.toDomain
import com.rbn.qtsettings.feature.dns.data.toPersisted
import com.rbn.qtsettings.feature.dns.domain.*
import com.rbn.qtsettings.platform.dns.AndroidDnsSystemGateway
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.NetworkDnsAutomation
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.VpnDetectionUtils

class PrivateDnsTileService : TileService() {
    private val dnsActions by lazy { DnsActions(AndroidDnsSystemGateway(this)) }
    private val dnsRevert by lazy { DnsAutoRevert(dnsActions) }

    private lateinit var prefsManager: PreferencesManager
    private var revertTimer: CountDownTimer? = null
    private val servicePrefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("dns_tile_service_state", MODE_PRIVATE)
    }

    private var isVpnConnected = false
    private var vpnMonitorTimer: CountDownTimer? = null
    private var currentNetworkState = DetectedNetworkState()
    @Volatile private var networkGeneration = 0L
    private var networkTypeCallback: ConnectivityManager.NetworkCallback? = null
    private var networkTypeMonitorTimer: CountDownTimer? = null
    private var monitorWifiSsid = false

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

    private val vpnAutomation get() = (application as QuickTileSettingsApplication).container.vpnAutomation

    private fun initializeVpnState() {
        if (!prefsManager.isVpnDetectionEnabled()) {
            vpnAutomation.clear()
            isVpnConnected = false
            return
        }
        if (prefsManager.getVpnDetectionMode() != TILE_ONLY_DETECTION) {
            isVpnConnected = VpnDetectionUtils.isVpnActive(this)
            return
        }
        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) return
        isVpnConnected = VpnDetectionUtils.isVpnActive(this)
        vpnAutomation.initialize(isVpnConnected, ::applyCurrentNetworkPolicy)
    }

    private fun onVpnConnected() = vpnAutomation.connected()
    private fun onVpnDisconnected() {
        vpnAutomation.disconnected(::applyCurrentNetworkPolicy, applyWithoutSavedState = true)
    }

    private fun applyCurrentNetworkPolicy(): Boolean {
        if (!prefsManager.isNetworkTypeDetectionEnabled()) return false
        val includeWifiIdentity = prefsManager.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION && prefsManager.areWifiNetworkRulesEnabled()
        return NetworkDnsAutomation.apply(this, prefsManager.automationStore,
            NetworkTypeDetectionUtils.getCurrentNetworkState(this, includeWifiIdentity))
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
        val isBackgroundMode =
            (prefsManager.isVpnDetectionEnabled() &&
                    prefsManager.getVpnDetectionMode() == BACKGROUND_DETECTION) ||
                    (prefsManager.isNetworkTypeDetectionEnabled() &&
                            prefsManager.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION)

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

        if (prefsManager.isDnsRequireUnlockEnabled() && isLocked) {
            unlockAndRun { performDnsToggle() }
            return
        }

        performDnsToggle()
    }

    private fun performDnsToggle() {
        val settings = prefsManager.dnsStore.read()
        val cycle = dnsActions.cycle(settings)
        if (cycle.result != DnsActionResult.Success) {
            val message = when (cycle.result) {
                DnsActionResult.NoCycleStates -> R.string.toast_no_states_enabled_dns
                DnsActionResult.InvalidHostname -> R.string.toast_hostname_required
                DnsActionResult.PermissionMissing -> R.string.toast_permission_not_granted_adb
                else -> R.string.toast_error_saving_settings
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            updateTile()
            return
        }
        val previous = requireNotNull(cycle.previous)
        val persisted = previous.toPersisted()
        savePreviousState(persisted.mode, persisted.hostname)
        if (settings.enableAutoRevert && settings.autoRevertDelaySeconds > 0) {
            dnsRevert.schedule(previous, cycle.generation)
            startRevertTimer(settings.autoRevertDelaySeconds)
            Toast.makeText(this, getString(
                R.string.toast_reverting_dns_to,
                getReadableDnsMode(persisted.mode, persisted.hostname),
                settings.autoRevertDelaySeconds
            ), Toast.LENGTH_LONG).show()
        } else {
            dnsRevert.cancel()
            clearPreviousState()
        }
        updateTile()
    }

    private fun startRevertTimer(delaySeconds: Int) {
        revertTimer?.cancel()
        revertTimer = object : CountDownTimer(delaySeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!dnsRevert.isCurrent()) {
                    cancelRevertTimerWithMessage(null)
                    return
                }
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
                if (!dnsRevert.isCurrent()) {
                    clearPreviousState()
                    revertTimer = null
                    updateTile()
                    return
                }
                getPreviousState()?.let { (prevMode, prevHost) ->
                    try {
                        if (dnsRevert.finish() != DnsActionResult.Success) return
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
        dnsRevert.cancel()
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
                            val target = PrivateDnsTarget(prevMode, prevHost).toDomain()
                            if (target == null || dnsActions.set(target) != DnsActionResult.Success) return
                            clearPreviousState()
                            updateTile()
                            return
                        }
                    }
                    if (prefsManager.isDnsToggleOffEnabled()) {
                        if (dnsActions.set(DnsTarget.Off) != DnsActionResult.Success) return
                        updateTile()
                        return
                    } else if (prefsManager.isDnsToggleAutoEnabled()) {
                        if (dnsActions.set(DnsTarget.Automatic) != DnsActionResult.Success) return
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
            currentNetworkState = DetectedNetworkState()
            return
        }

        val detectionMode = prefsManager.getNetworkTypeDetectionMode()
        if (detectionMode != TILE_ONLY_DETECTION) {
            return
        }

        if (!PermissionUtils.hasWriteSecureSettingsPermission(this)) {
            return
        }

        // SSID rules require the location foreground service used by background detection.
        // A TileService cannot reliably receive location-sensitive WifiInfo on Android 10+.
        monitorWifiSsid = false
        val detectedState = NetworkTypeDetectionUtils.getCurrentNetworkState(
            context = this,
            includeWifiSsid = monitorWifiSsid
        )
        applyNetworkStateIfChanged(detectedState, force = true)
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

        stopNetworkTypeMonitoring()
        monitorWifiSsid = false
        val registeredGeneration = networkGeneration
        val mainHandler = Handler(Looper.getMainLooper())
        networkTypeCallback = NetworkTypeDetectionUtils.createNetworkStateCallback(
            context = this,
            includeWifiSsid = monitorWifiSsid,
            isCurrent = { registeredGeneration == networkGeneration },
            onNetworkStateChanged = { detectedNetworkState ->
                mainHandler.post {
                    if (registeredGeneration == networkGeneration) applyNetworkStateIfChanged(detectedNetworkState)
                }
            }
        )

        networkTypeCallback?.let { callback ->
            NetworkTypeDetectionUtils.registerNetworkTypeCallback(this, callback)
        }

        networkTypeMonitorTimer =
            object : CountDownTimer(Long.MAX_VALUE, 2000) {
                override fun onTick(millisUntilFinished: Long) {
                    applyNetworkStateIfChanged(
                        NetworkTypeDetectionUtils.getCurrentNetworkState(
                            context = this@PrivateDnsTileService,
                            includeWifiSsid = monitorWifiSsid
                        )
                    )
                }

                override fun onFinish() {
                }
            }.start()
    }

    private fun stopNetworkTypeMonitoring() {
        networkGeneration++
        networkTypeCallback?.let { callback ->
            NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(this, callback)
        }
        networkTypeCallback = null
        networkTypeMonitorTimer?.cancel()
        networkTypeMonitorTimer = null
    }

    private fun applyNetworkStateIfChanged(
        detectedNetworkState: DetectedNetworkState,
        force: Boolean = false
    ) {
        if (!force && detectedNetworkState == currentNetworkState) return
        currentNetworkState = detectedNetworkState
        handleNetworkStateChange(detectedNetworkState)
        updateTile()
    }

    private fun handleNetworkStateChange(newNetworkState: DetectedNetworkState) {
        Log.i(
            "PrivateDnsTile",
            "Network state changed to ${newNetworkState.networkType}; Wi-Fi identity available=" +
                    (newNetworkState.wifiSsid != null)
        )

        if (prefsManager.isVpnDetectionEnabled() && VpnDetectionUtils.isVpnActive(this)) {
            Log.d(
                "PrivateDnsTile",
                "VPN is active and VPN detection is enabled, skipping network type DNS change"
            )
            return
        }

        NetworkDnsAutomation.apply(this, prefsManager, newNetworkState)
    }
}
