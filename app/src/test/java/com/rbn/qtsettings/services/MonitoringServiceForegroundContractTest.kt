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
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
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
        val serviceJobField = NetworkMonitoringService::class.java
            .getDeclaredField("serviceJob")
            .apply { isAccessible = true }

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", false),
            0,
            201
        )
        val initialJob = serviceJobField.get(service)

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", false),
            0,
            202
        )
        assertSame(initialJob, serviceJobField.get(service))

        service.onStartCommand(
            Intent(context, service::class.java).putExtra("reapply_policy", true),
            0,
            203
        )
        assertNotSame(initialJob, serviceJobField.get(service))
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
            val callback = NetworkMonitoringService::class.java.getDeclaredField("networkCallback")
                .apply { isAccessible = true }.get(service) as ConnectivityManager.NetworkCallback
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

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
