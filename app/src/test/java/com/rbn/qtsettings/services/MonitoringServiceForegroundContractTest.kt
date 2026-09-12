package com.rbn.qtsettings.services

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import android.provider.Settings
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.receivers.BootCompletedReceiver
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.NetworkTypeDetectionUtils
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowWifiInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonitoringServiceForegroundContractTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        preferencesManager = PreferencesManager.getInstance(context)
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        )
    }

    @After
    fun tearDown() {
        shadowOf(context as Application)
            .denyPermissions(
                android.Manifest.permission.WRITE_SECURE_SETTINGS,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun networkMonitoringService_withoutWriteSecureSettings_promotesBeforeStopping() {
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        val controller = Robolectric.buildService(NetworkMonitoringService::class.java).create()
        val service = controller.get()

        val result = service.onStartCommand(Intent(context, service::class.java), 0, 101)

        val shadowService = shadowOf(service)
        assertEquals(Service.START_NOT_STICKY, result)
        assertNotNull(shadowService.lastForegroundNotification)
        assertTrue(shadowService.isStoppedBySelf)
        assertEquals(101, shadowService.stopSelfId)
        controller.destroy()
    }

    @Test
    fun vpnMonitoringService_withoutWriteSecureSettings_promotesBeforeStopping() {
        preferencesManager.setVpnDetectionEnabled(true)
        preferencesManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        val controller = Robolectric.buildService(VpnMonitoringService::class.java).create()
        val service = controller.get()

        val result = service.onStartCommand(Intent(context, service::class.java), 0, 102)

        val shadowService = shadowOf(service)
        assertEquals(Service.START_NOT_STICKY, result)
        assertNotNull(shadowService.lastForegroundNotification)
        assertTrue(shadowService.isStoppedBySelf)
        assertEquals(102, shadowService.stopSelfId)
        controller.destroy()
    }

    @Test
    fun networkMonitoringService_initializationStartDoesNotRestartActiveMonitoring() {
        shadowOf(context as Application)
            .grantPermissions(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        val controller = Robolectric.buildService(NetworkMonitoringService::class.java).create()
        val service = controller.get()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", false),
            0,
            201
        )
        val initialJob = shadowOf(connectivity).networkCallbacks.single()

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", false),
            0,
            202
        )
        assertSame(initialJob, shadowOf(connectivity).networkCallbacks.single())

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", true),
            0,
            203
        )
        assertNotSame(initialJob, shadowOf(connectivity).networkCallbacks.single())
        controller.destroy()
    }

    @Test
    fun bootCompleted_withLocationAccess_appliesWifiRuleWithoutOpeningActivity() {
        shadowOf(context as Application).grantPermissions(
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        shadowOf(context.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setWifiNetworkRulesEnabled(true)
        preferencesManager.setDnsStateOnWifi(DNS_MODE_AUTO)
        preferencesManager.addWifiNetworkRule(WifiDnsRule(ssid = "Campus", actionMode = DNS_MODE_OFF))
        Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_ON)
        shadowOf(context as Application).clearStartedServices()

        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        val startIntent = shadowOf(context as Application).allStartedServices.single()
        assertEquals(NetworkMonitoringService::class.java.name, startIntent.component?.className)
        val controller = Robolectric.buildService(NetworkMonitoringService::class.java).create()
        val service = controller.get()
        try {
            service.onStartCommand(startIntent, 0, 301)
            assertTrue(service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION != 0)
            val callback = shadowOf(context.getSystemService(ConnectivityManager::class.java)).networkCallbacks.single()
            val wifiInfo = ShadowWifiInfo.newInstance()
            shadowOf(wifiInfo).setSSID("Campus")
            shadowOf(wifiInfo).setBSSID("AA:BB:CC:DD:EE:FF")
            val capabilities = ShadowNetworkCapabilities.newInstance()
            shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(capabilities).setTransportInfo(wifiInfo)
            callback.onCapabilitiesChanged(ShadowNetwork.newInstance(301), capabilities)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))

            // A second network with the same SSID still matches; a different SSID restores default.
            shadowOf(wifiInfo).setBSSID("11:22:33:44:55:66")
            callback.onCapabilitiesChanged(ShadowNetwork.newInstance(301), capabilities)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
            shadowOf(wifiInfo).setSSID("Home")
            callback.onCapabilitiesChanged(ShadowNetwork.newInstance(301), capabilities)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_AUTO, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun wifiRule_waitsForVpnOverrideToEndEvenWhenWifiIdentityDoesNotChange() {
        shadowOf(context as Application).grantPermissions(
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        shadowOf(context.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        preferencesManager.setVpnDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setWifiNetworkRulesEnabled(true)
        preferencesManager.addWifiNetworkRule(
            WifiDnsRule(ssid = "Campus", actionMode = DNS_MODE_ON, dnsHostname = "wifi.example.com")
        )
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = requireNotNull(connectivity.activeNetwork)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_VPN)
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val wifiInfo = ShadowWifiInfo.newInstance()
        shadowOf(wifiInfo).setSSID("Campus")
        shadowOf(capabilities).setTransportInfo(wifiInfo)
        shadowOf(connectivity).setNetworkCapabilities(network, capabilities)
        Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_OFF)
        val controller = Robolectric.buildService(NetworkMonitoringService::class.java).create()
        try {
            controller.get().onStartCommand(Intent(context, NetworkMonitoringService::class.java), 0, 401)
            val callback = shadowOf(connectivity).networkCallbacks.single()
            callback.onCapabilitiesChanged(network, capabilities)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))

            shadowOf(capabilities).removeTransportType(NetworkCapabilities.TRANSPORT_VPN)
            callback.onCapabilitiesChanged(network, capabilities)
            // The physical Wi-Fi identity is unchanged, so its callback is deduplicated.
            // The service's three-second poll notices that the VPN override ended.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            assertEquals(DNS_MODE_ON, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
            assertEquals("wifi.example.com", Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER))
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun vpnRecovery_prefersCurrentNetworkPolicyOverSavedDnsState() {
        shadowOf(context as Application).grantPermissions(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        preferencesManager.setVpnDetectionEnabled(true)
        preferencesManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setDnsStateOnWifi(DNS_MODE_AUTO)
        context.getSharedPreferences("vpn_detection_shared_state", Context.MODE_PRIVATE).edit()
            .putString("vpn_previous_dns_mode", DNS_MODE_ON)
            .putString("vpn_previous_dns_hostname", "before-vpn.example.com")
            .commit()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(connectivity).setNetworkCapabilities(requireNotNull(connectivity.activeNetwork), capabilities)
        Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_OFF)
        val controller = Robolectric.buildService(VpnMonitoringService::class.java).create()
        try {
            controller.get().onStartCommand(Intent(context, VpnMonitoringService::class.java), 0, 402)
            assertEquals(DNS_MODE_AUTO, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        } finally {
            controller.destroy()
        }
    }

    private fun clearPreferences() {
        context.getSharedPreferences("vpn_detection_shared_state", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun obsoleteNetworkCallbacks_cannotUndoManualDnsAfterRestartOrDestroy() {
        shadowOf(context as Application).grantPermissions(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setDnsStateOnWifi(DNS_MODE_OFF)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        shadowOf(connectivity).setNetworkCapabilities(requireNotNull(connectivity.activeNetwork), ShadowNetworkCapabilities.newInstance())
        val controller = Robolectric.buildService(NetworkMonitoringService::class.java).create()
        val service = controller.get()
        try {
            service.onStartCommand(Intent(context, service::class.java), 0, 501)
            val obsolete = shadowOf(connectivity).networkCallbacks.single()
            service.onStartCommand(Intent(context, service::class.java), 0, 502)
            val current = shadowOf(connectivity).networkCallbacks.single()
            assertNotSame(obsolete, current)
            Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_ON)
            val wifi = ShadowNetworkCapabilities.newInstance()
            shadowOf(wifi).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            shadowOf(wifi).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            obsolete.onCapabilitiesChanged(ShadowNetwork.newInstance(501), wifi)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_ON, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))

            assertEquals(NETWORK_TYPE_NONE, NetworkTypeDetectionUtils.getCurrentNetworkType(context, preferTrackedWifi = true))
            current.onCapabilitiesChanged(ShadowNetwork.newInstance(502), wifi)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
            controller.destroy()
            assertTrue(shadowOf(connectivity).networkCallbacks.isEmpty())
            Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_ON)
            current.onLost(ShadowNetwork.newInstance(502))
            current.onCapabilitiesChanged(ShadowNetwork.newInstance(503), wifi)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            assertEquals(DNS_MODE_ON, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun pendingVpnDisconnect_isCancelledWhenServiceIsDestroyed() {
        shadowOf(context as Application).grantPermissions(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        preferencesManager.setVpnDetectionEnabled(true)
        preferencesManager.setVpnDetectionMode(BACKGROUND_DETECTION)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val network = requireNotNull(connectivity.activeNetwork)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_VPN)
        shadowOf(connectivity).setNetworkCapabilities(network, capabilities)
        Settings.Global.putString(context.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_AUTO)
        val controller = Robolectric.buildService(VpnMonitoringService::class.java).create()
        controller.get().onStartCommand(Intent(context, VpnMonitoringService::class.java), 0, 601)
        val callback = shadowOf(connectivity).networkCallbacks.single()
        assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        shadowOf(capabilities).removeTransportType(NetworkCapabilities.TRANSPORT_VPN)
        callback.onLost(network)
        shadowOf(Looper.getMainLooper()).idle()
        controller.destroy()
        assertTrue(shadowOf(connectivity).networkCallbacks.isEmpty())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        assertEquals(DNS_MODE_AUTO, context.getSharedPreferences("vpn_detection_shared_state", Context.MODE_PRIVATE)
            .getString("vpn_previous_dns_mode", null))
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
