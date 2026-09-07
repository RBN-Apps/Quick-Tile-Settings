package com.rbn.qtsettings.services

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.ADB_ENABLED
import com.rbn.qtsettings.utils.Constants.ADB_WIFI_ENABLED
import com.rbn.qtsettings.utils.Constants.DEVELOPMENT_SETTINGS_ENABLED
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.SystemQuickActionResult
import com.rbn.qtsettings.utils.SystemQuickActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowTileService
import java.time.Duration

/** Exercises real tile entry points and timers without changing production seams. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [TileActionCharacterizationTest.DestroySafeTileServiceShadow::class])
@LooperMode(LooperMode.Mode.PAUSED)
class TileActionCharacterizationTest {
    // Robolectric 4.16.1's ShadowTileService does not extend ShadowService, so the
    // framework's empty onDestroy() casts to an incompatible shadow. Replace only
    // that framework callback; the application's timer cleanup still runs normally.
    @Implements(TileService::class)
    class DestroySafeTileServiceShadow : ShadowTileService() {
        @Implementation
        protected fun onDestroy() = Unit
    }

    private lateinit var application: Application
    private lateinit var preferences: PreferencesManager

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        resetPreferences()
        preferences = PreferencesManager.getInstance(application)
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        preferences.setDnsRequireUnlock(false)
        preferences.setUsbRequireUnlock(false)
        preferences.setDnsEnableAutoRevert(false)
        preferences.setUsbEnableAutoRevert(false)
        preferences.setUsbAlsoHideDevOptions(false)
        preferences.setUsbAlsoDisableWirelessDebugging(false)
        preferences.setUsbToggleEnable(true)
        preferences.setUsbToggleDisable(true)
        Settings.Global.putInt(application.contentResolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
        Settings.Global.putInt(application.contentResolver, ADB_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, ADB_WIFI_ENABLED, 0)
        Settings.Global.putString(application.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_OFF)
        Settings.Global.putString(application.contentResolver, PRIVATE_DNS_SPECIFIER, "old.example.com")
    }

    @After
    fun tearDown() {
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        resetPreferences()
    }

    @Test
    fun usbTile_cyclesOnThenOff() = withUsbTile { tile ->
        tile.onClick()
        assertEquals(1, globalInt(ADB_ENABLED))
        tile.onClick()
        assertEquals(0, globalInt(ADB_ENABLED))
    }

    @Test
    fun usbTile_emptySelectionDoesNotChangeAnySystemSetting() = withUsbTile { tile ->
        preferences.setUsbToggleEnable(false)
        preferences.setUsbToggleDisable(false)
        preferences.setUsbAlsoHideDevOptions(true)
        preferences.setUsbAlsoDisableWirelessDebugging(true)
        tile.onClick()
        assertEquals(0, globalInt(ADB_ENABLED))
        assertEquals(1, globalInt(DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, globalInt(ADB_WIFI_ENABLED))
    }

    @Test
    fun usbTile_singleEnabledStateStaysSelectedOnRepeatedClicks() = withUsbTile { tile ->
        preferences.setUsbToggleDisable(false)
        tile.onClick()
        tile.onClick()
        assertEquals(1, globalInt(ADB_ENABLED))
    }

    @Test
    fun usbTile_hiddenDeveloperOptionsTreatStaleAdbFlagAsOff() = withUsbTile { tile ->
        preferences.setUsbAlsoHideDevOptions(true)
        preferences.setUsbAlsoDisableWirelessDebugging(true)
        Settings.Global.putInt(application.contentResolver, DEVELOPMENT_SETTINGS_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, ADB_ENABLED, 1)
        tile.onClick()
        assertEquals(1, globalInt(DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(1, globalInt(ADB_ENABLED))
        assertEquals(1, globalInt(ADB_WIFI_ENABLED))
    }

    @Test
    fun usbAutoRevert_restoresIndependentCompanionStates() = withUsbTile { tile ->
        enableUsbAutoRevert()
        preferences.setUsbAlsoHideDevOptions(true)
        preferences.setUsbAlsoDisableWirelessDebugging(true)
        tile.onClick()
        assertEquals(1, globalInt(ADB_ENABLED))
        assertEquals(1, globalInt(ADB_WIFI_ENABLED))
        advancePastRevert()
        assertEquals(0, globalInt(ADB_ENABLED))
        assertEquals(1, globalInt(DEVELOPMENT_SETTINGS_ENABLED))
        assertEquals(0, globalInt(ADB_WIFI_ENABLED))
    }

    @Test
    fun usbAutoRevert_doesNotOverwriteLaterDirectAction() = withUsbTile { tile ->
        enableUsbAutoRevert()
        tile.onClick()
        assertEquals(SystemQuickActionResult.SUCCESS, setUsbDirectly(true))
        advancePastRevert()
        assertEquals(1, globalInt(ADB_ENABLED))
    }

    @Test
    fun rejectedUsbAction_doesNotCancelPendingAutoRevert() = withUsbTile { tile ->
        enableUsbAutoRevert()
        tile.onClick()
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        assertEquals(SystemQuickActionResult.PERMISSION_MISSING, setUsbDirectly(true))
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        advancePastRevert()
        assertEquals(0, globalInt(ADB_ENABLED))
    }

    @Test
    fun dnsAutoRevert_restoresPreviousModeAndHostname() = withDnsTile { tile ->
        Settings.Global.putString(application.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_ON)
        tile.onClick()
        assertEquals(DNS_MODE_AUTO, globalString(PRIVATE_DNS_MODE))
        advancePastRevert()
        assertEquals(DNS_MODE_ON, globalString(PRIVATE_DNS_MODE))
        assertEquals("old.example.com", globalString(PRIVATE_DNS_SPECIFIER))
    }

    @Test
    fun dnsAutoRevert_doesNotOverwriteLaterDirectProviderAction() = withDnsTile { tile ->
        tile.onClick()
        assertEquals(DNS_MODE_AUTO, globalString(PRIVATE_DNS_MODE))
        assertEquals(
            SystemQuickActionResult.SUCCESS,
            SystemQuickActions.setActiveDns(application, "new.example.com")
        )
        advancePastRevert()
        assertEquals(DNS_MODE_ON, globalString(PRIVATE_DNS_MODE))
        assertEquals("new.example.com", globalString(PRIVATE_DNS_SPECIFIER))
    }

    @Test
    fun directDnsAction_cancelsOnlyDnsRevertWhileUsbRevertStillRuns() = withDnsTile { dns ->
        withUsbTile { usb ->
            enableUsbAutoRevert()
            dns.onClick()
            usb.onClick()
            assertEquals(1, globalInt(ADB_ENABLED))
            assertEquals(
                SystemQuickActionResult.SUCCESS,
                SystemQuickActions.setDnsMode(application, DNS_MODE_AUTO)
            )
            advancePastRevert()
            assertEquals(DNS_MODE_AUTO, globalString(PRIVATE_DNS_MODE))
            assertEquals(0, globalInt(ADB_ENABLED))
        }
    }

    private fun withUsbTile(block: (UsbDebuggingTileService) -> Unit) {
        val controller = Robolectric.buildService(UsbDebuggingTileService::class.java).create()
        try {
            block(controller.get())
        } finally {
            controller.destroy()
        }
    }

    private fun withDnsTile(block: (PrivateDnsTileService) -> Unit) {
        preferences.setDnsToggleOff(false)
        preferences.setDnsToggleAuto(true)
        preferences.getAllDnsHostnamesBlocking().forEach {
            preferences.updateDnsHostnameEntry(it.copy(isSelectedForCycle = false))
        }
        preferences.setDnsEnableAutoRevert(true)
        preferences.setDnsAutoRevertDelaySeconds(2)
        val controller = Robolectric.buildService(PrivateDnsTileService::class.java).create()
        try {
            block(controller.get())
        } finally {
            controller.destroy()
        }
    }

    private fun enableUsbAutoRevert() {
        preferences.setUsbEnableAutoRevert(true)
        preferences.setUsbAutoRevertDelaySeconds(2)
    }

    private fun setUsbDirectly(enabled: Boolean) = SystemQuickActions.setUsbDebuggingEnabled(
        application, enabled, toggleDeveloperOptions = false, toggleWirelessDebugging = false
    )

    private fun advancePastRevert() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))

    private fun globalInt(key: String) = Settings.Global.getInt(application.contentResolver, key, 0)

    private fun globalString(key: String) = Settings.Global.getString(application.contentResolver, key)

    private fun resetPreferences() {
        listOf("qt_settings_prefs", "dns_tile_service_state", "usb_tile_service_state",
            "auto_revert_coordination").forEach { name ->
            application.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        PreferencesManager::class.java.getDeclaredField("INSTANCE").apply {
            isAccessible = true
            set(null, null)
        }
    }
}
