package com.rbn.qtsettings.receivers

import android.content.Context
import android.content.Intent
import com.rbn.qtsettings.data.PreferencesManager
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
    private lateinit var preferences: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        preferences = PreferencesManager.getInstance(context)
        shadowOf(RuntimeEnvironment.getApplication()).clearStartedServices()
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun onReceive_whenActionIsNotBootCompleted_startsNoServices() {
        preferences.setVpnDetectionEnabled(true)
        preferences.setVpnDetectionMode(BACKGROUND_DETECTION)

        BootCompletedReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertTrue(startedServiceClasses().isEmpty())
    }

    @Test
    fun onReceive_whenBackgroundDetectionIsDisabled_startsNoServices() {
        preferences.setVpnDetectionEnabled(true)
        preferences.setVpnDetectionMode(TILE_ONLY_DETECTION)
        preferences.setNetworkTypeDetectionEnabled(false)
        preferences.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

        sendBootCompleted()

        assertTrue(startedServiceClasses().isEmpty())
    }

    @Test
    fun onReceive_whenVpnBackgroundDetectionIsEnabled_startsVpnService() {
        preferences.setVpnDetectionEnabled(true)
        preferences.setVpnDetectionMode(BACKGROUND_DETECTION)

        sendBootCompleted()

        assertEquals(listOf(VpnMonitoringService::class.java), startedServiceClasses())
    }

    @Test
    fun onReceive_whenBothBackgroundDetectionsAreEnabled_startsBothServices() {
        preferences.setVpnDetectionEnabled(true)
        preferences.setVpnDetectionMode(BACKGROUND_DETECTION)
        preferences.setNetworkTypeDetectionEnabled(true)
        preferences.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)

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

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
