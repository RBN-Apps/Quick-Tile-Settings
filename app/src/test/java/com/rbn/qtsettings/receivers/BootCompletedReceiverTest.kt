package com.rbn.qtsettings.receivers

import android.content.Context
import android.content.Intent
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.services.NetworkMonitoringService
import com.rbn.qtsettings.services.VpnMonitoringService
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var preferences: AppStores

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetAppStoresSingleton()
        clearPreferences()
        preferences = AppStores.getInstance(context)
        shadowOf(RuntimeEnvironment.getApplication()).clearStartedServices()
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetAppStoresSingleton()
    }

    @Test
    fun onReceive_whenActionIsNotBootCompleted_startsNoServices() {
        preferences.automation.setVpnDetectionEnabled(true)
        preferences.automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertTrue(startedServiceClasses().isEmpty())
    }

    @Test
    fun onReceive_whenBackgroundDetectionIsDisabled_startsNoServices() {
        preferences.automation.setVpnDetectionEnabled(true)
        preferences.automation.setVpnDetectionMode(TILE_ONLY_DETECTION)
        preferences.automation.setNetworkTypeDetectionEnabled(false)
        preferences.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        sendBootCompleted()

        assertTrue(startedServiceClasses().isEmpty())
    }

    @Test
    fun onReceive_whenVpnBackgroundDetectionIsEnabled_startsVpnService() {
        preferences.automation.setVpnDetectionEnabled(true)
        preferences.automation.setVpnDetectionMode(BACKGROUND_DETECTION)

        sendBootCompleted()

        assertEquals(listOf(VpnMonitoringService::class.java), startedServiceClasses())
    }

    @Test
    fun onReceive_whenBothBackgroundDetectionsAreEnabled_startsBothServices() {
        preferences.automation.setVpnDetectionEnabled(true)
        preferences.automation.setVpnDetectionMode(BACKGROUND_DETECTION)
        preferences.automation.setNetworkTypeDetectionEnabled(true)
        preferences.automation.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        sendBootCompleted()

        assertEquals(
            setOf(VpnMonitoringService::class.java, NetworkMonitoringService::class.java),
            startedServiceClasses().toSet()
        )
    }

    private fun sendBootCompleted() {
        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    }

    private fun startedServiceClasses(): List<Class<*>> =
        shadowOf(RuntimeEnvironment.getApplication())
            .allStartedServices
            .mapNotNull { it.component?.className }
            .map { Class.forName(it) }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetAppStoresSingleton() {
        val instanceField = com.rbn.qtsettings.app.AppStores::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
