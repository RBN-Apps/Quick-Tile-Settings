package com.rbn.qtsettings.services

import android.content.Context
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
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.dns.data.toDomain
import com.rbn.qtsettings.feature.dns.data.toPersisted
import com.rbn.qtsettings.feature.dns.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.platform.automation.NetworkDnsAutomation
import com.rbn.qtsettings.platform.automation.NetworkTypeDetectionUtils
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import com.rbn.qtsettings.platform.automation.VpnDetectionUtils
import com.rbn.qtsettings.feature.dns.presentation.dnsIconForHostname

private const val SERVICE_STATE_PREFERENCES = "dns_tile_service_state"
private const val KEY_DNS_PREVIOUS_MODE_FOR_REVERT = "dns_previous_mode_for_revert"
private const val KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT = "dns_previous_hostname_for_revert"

class PrivateDnsTileService : TileService() {
    private val container get() = (application as QuickTileSettingsApplication).container
    // Share the process-wide action and revert graph so a pending revert survives this service
    // and so direct actions from the app cancel it.
    private val dnsActions by lazy { container.dnsActions }
    private val dnsRevert by lazy { container.dnsAutoRevert }

    private lateinit var dnsStore: com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
    private lateinit var automationStore: com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
    private var subtitleScope: CoroutineScope? = null
    private val servicePrefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(SERVICE_STATE_PREFERENCES, MODE_PRIVATE)
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
        val stores = AppStores.getInstance(applicationContext)
        dnsStore = stores.dns
        automationStore = stores.automation
    }

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { subtitleScope = it }
        scope.launch {
            dnsRevert.remainingSeconds.collectLatest { remaining ->
                if (remaining == null) updateTile() else renderSubtitle(remaining)
            }
        }
        initializeVpnState()
        startVpnMonitoring()
        initializeNetworkTypeState()
        startNetworkTypeMonitoring()
        startObservingDnsSettings()
        updateTile()
    }

    private fun savePreviousState(mode: String, hostname: String?) {
        servicePrefs.edit {
            putString(KEY_DNS_PREVIOUS_MODE_FOR_REVERT, mode)
                .putString(KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT, hostname)
        }
    }

    private fun getPreviousState(): Pair<String, String?>? {
        val mode = servicePrefs.getString(KEY_DNS_PREVIOUS_MODE_FOR_REVERT, null)
        return mode?.let {
            Pair(
                it,
                servicePrefs.getString(
                    KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT,
                    null
                )
            )
        }
    }

    private fun clearPreviousState() {
        servicePrefs.edit {
            remove(KEY_DNS_PREVIOUS_MODE_FOR_REVERT)
                .remove(KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT)
        }
    }

    private val vpnAutomation get() = (application as QuickTileSettingsApplication).container.vpnAutomation

    private fun initializeVpnState() {
        if (!automationStore.isVpnDetectionEnabled()) {
            vpnAutomation.clear()
            isVpnConnected = false
            return
        }
        if (automationStore.getVpnDetectionMode() != TILE_ONLY_DETECTION) {
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
        if (!automationStore.isNetworkTypeDetectionEnabled()) return false
        val includeWifiIdentity = automationStore.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION && automationStore.areWifiNetworkRulesEnabled()
        return NetworkDnsAutomation.apply(this, automationStore,
            NetworkTypeDetectionUtils.getCurrentNetworkState(this, includeWifiIdentity))
    }

    private fun startVpnMonitoring() {
        if (!automationStore.isVpnDetectionEnabled() || automationStore.getVpnDetectionMode() != TILE_ONLY_DETECTION) {
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
            (automationStore.isVpnDetectionEnabled() &&
                    automationStore.getVpnDetectionMode() == BACKGROUND_DETECTION) ||
                    (automationStore.isNetworkTypeDetectionEnabled() &&
                            automationStore.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION)

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

        if (dnsStore.read().requireUnlock && isLocked) {
            unlockAndRun { performDnsToggle() }
            return
        }

        performDnsToggle()
    }

    private fun performDnsToggle() {
        val settings = dnsStore.read()
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
            // Capture the application context only: this callback can outlive the service.
            val context = applicationContext
            val readableNames = dnsStore::getAllDnsHostnamesBlocking
            dnsRevert.schedule(previous, cycle.generation, settings.autoRevertDelaySeconds) { restored, restoredTo ->
                announceRevert(context, restored, restoredTo, readableNames)
            }
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

    /** The countdown now lives outside this service, so the tile only renders its ticks. */
    private fun renderSubtitle(remaining: Int) {
        val tile = qsTile ?: return
        val previous = dnsRevert.previous?.toPersisted() ?: return
        tile.subtitle = getString(
            R.string.tile_subtitle_reverting_in_seconds,
            getReadableDnsMode(previous.mode, previous.hostname),
            remaining
        )
        tile.updateTile()
    }

    private companion object {
        /** Static so a revert landing after the service is gone keeps nothing alive. */
        fun announceRevert(
            context: Context,
            restored: DnsActionResult?,
            previous: DnsTarget,
            names: () -> List<DnsHostnameEntry>
        ) {
            context.getSharedPreferences(SERVICE_STATE_PREFERENCES, MODE_PRIVATE).edit {
                remove(KEY_DNS_PREVIOUS_MODE_FOR_REVERT).remove(KEY_DNS_PREVIOUS_HOSTNAME_FOR_REVERT)
            }
            if (restored != DnsActionResult.Success) return
            val persisted = previous.toPersisted()
            Log.i("PrivateDnsTile", "Auto-reverted DNS to " + persisted.mode)
            val readable = when (persisted.mode) {
                DNS_MODE_OFF -> context.getString(R.string.off_state)
                DNS_MODE_AUTO -> context.getString(R.string.auto_state)
                DNS_MODE_ON -> persisted.hostname?.let { host ->
                    names().find { it.hostname == host }?.name ?: host
                } ?: context.getString(R.string.on_state)
                else -> persisted.mode
            }
            Toast.makeText(context, context.getString(R.string.dns_state_reverted_to, readable),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelRevertTimerWithMessage(message: String?) {
        if (!dnsRevert.cancel()) return
        clearPreviousState()
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        Log.i("PrivateDnsTile", "Revert timer cancelled.")
        updateTile()
    }

    private fun getReadableDnsMode(mode: String, hostname: String? = null): String {
        return when (mode) {
            DNS_MODE_OFF -> getString(R.string.off_state)
            DNS_MODE_AUTO -> getString(R.string.auto_state)
            DNS_MODE_ON -> {
                if (hostname != null) {
                    val entry =
                        dnsStore.getAllDnsHostnamesBlocking().find { it.hostname == hostname }
                    entry?.name ?: hostname
                } else {
                    getString(R.string.on_state)
                }
            }

            else -> mode
        }
    }

    private fun getDnsIcon(hostname: String?): Int = dnsIconForHostname(hostname)

    private fun updateTile() {
        val tile = qsTile ?: return

        if (dnsRevert.remainingSeconds.value == null || getPreviousState() == null) {
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
                    val entry = dnsStore.getAllDnsHostnamesBlocking()
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
                    if (dnsStore.read().toggleOff) {
                        if (dnsActions.set(DnsTarget.Off) != DnsActionResult.Success) return
                        updateTile()
                        return
                    } else if (dnsStore.read().toggleAuto) {
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
        subtitleScope?.cancel()
        subtitleScope = null
        stopVpnMonitoring()
        stopNetworkTypeMonitoring()
        stopObservingDnsSettings()
    }

    // The pending revert deliberately outlives this service now; only the rendering stops.
    override fun onDestroy() {
        super.onDestroy()
        subtitleScope?.cancel()
        subtitleScope = null
        stopVpnMonitoring()
        stopNetworkTypeMonitoring()
        stopObservingDnsSettings()
    }

    private fun initializeNetworkTypeState() {
        if (!automationStore.isNetworkTypeDetectionEnabled()) {
            currentNetworkState = DetectedNetworkState()
            return
        }

        val detectionMode = automationStore.getNetworkTypeDetectionMode()
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
        if (!automationStore.isNetworkTypeDetectionEnabled() ||
            automationStore.getNetworkTypeDetectionMode() != TILE_ONLY_DETECTION
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

        if (automationStore.isVpnDetectionEnabled() && VpnDetectionUtils.isVpnActive(this)) {
            Log.d(
                "PrivateDnsTile",
                "VPN is active and VPN detection is enabled, skipping network type DNS change"
            )
            return
        }

        NetworkDnsAutomation.apply(this, automationStore, newNetworkState)
    }
}
