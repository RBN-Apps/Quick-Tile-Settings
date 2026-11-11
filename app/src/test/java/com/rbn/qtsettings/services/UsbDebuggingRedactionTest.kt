package com.rbn.qtsettings.services

import android.Manifest
import android.app.Application
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rbn.qtsettings.QuickTileSettingsApplication
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.DebuggingSettingsProvider
import org.junit.After
import org.junit.Assert.*
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
import org.robolectric.shadows.ShadowTileService
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [UsbDebuggingRedactionTest.DestroySafeTileServiceShadow::class])
class UsbDebuggingRedactionTest {
    // Robolectric 4.16.1's ShadowTileService does not extend ShadowService.
    // Avoid its incompatible framework onDestroy cast; app cleanup still runs.
    @Implements(TileService::class)
    class DestroySafeTileServiceShadow : ShadowTileService() {
        @Implementation
        protected fun onDestroy() = Unit
    }

    private lateinit var application: Application
    private lateinit var prefs: UsbSettingsStore
    private lateinit var provider: DebuggingSettingsProvider

    @Before fun setUp() {
        application = RuntimeEnvironment.getApplication()
        resetPreferences()
        prefs = AppStores.getInstance(application).usb
        prefs.setToggleEnable(true)
        prefs.setToggleDisable(true)
        prefs.setAlsoHideDevOptions(true)
        prefs.setAlsoDisableWirelessDebugging(true)
        prefs.setEnableAutoRevert(false)
        prefs.setRequireUnlock(false)
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        provider = DebuggingSettingsProvider().apply { register(application) }
        Settings.Global.putInt(application.contentResolver, Constants.ADB_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, Constants.DEVELOPMENT_SETTINGS_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, Constants.ADB_WIFI_ENABLED, 1)
        provider.values[Constants.ADB_ENABLED] = "1"
        provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "1"
    }

    @After fun tearDown() {
        (application as QuickTileSettingsApplication).container.usbAutoRevert.cancel()
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        resetPreferences()
    }

    @Test fun tileCyclesFromActualOnToOff_thenBackOn() {
        val controller = Robolectric.buildService(UsbDebuggingTileService::class.java).create()
        try {
            controller.get().onClick()
            assertWrittenState(0)

            provider.values[Constants.ADB_ENABLED] = "0"
            provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "0"
            controller.get().onClick()
            assertWrittenState(1)
        } finally { controller.destroy() }
    }

    @Test fun tileDisplaysActualEnabledState() {
        val controller = Robolectric.buildService(UsbDebuggingTileService::class.java).create()
        try {
            controller.get().onStartListening()
            assertEquals(Tile.STATE_ACTIVE, controller.get().qsTile.state)
            controller.get().onStopListening()
            provider.values[Constants.ADB_ENABLED] = "0"
            controller.get().onStartListening()
            assertEquals(Tile.STATE_INACTIVE, controller.get().qsTile.state)
        } finally { controller.destroy() }
    }

    @Test fun tileWithoutDeveloperOptionsCompanion_isNotBlockedByRedaction() {
        prefs.setAlsoHideDevOptions(false)
        provider.values[Constants.ADB_ENABLED] = "0"
        val controller = Robolectric.buildService(UsbDebuggingTileService::class.java).create()
        try {
            controller.get().onClick()
            assertEquals(1, Settings.Global.getInt(application.contentResolver, Constants.ADB_ENABLED, 0))
        } finally { controller.destroy() }
    }

    @Test fun autoRevert_restoresActualOnState_insteadOfRedactedOff() {
        prefs.setEnableAutoRevert(true)
        prefs.setAutoRevertDelaySeconds(5)
        val controller = Robolectric.buildService(UsbDebuggingTileService::class.java).create()
        try {
            controller.get().onClick()
            assertWrittenState(0)
            provider.values[Constants.ADB_ENABLED] = "0"
            provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "0"

            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
            assertWrittenState(1)
        } finally { controller.destroy() }
    }

    private fun assertWrittenState(expected: Int) {
        for (key in listOf(Constants.ADB_ENABLED, Constants.DEVELOPMENT_SETTINGS_ENABLED, Constants.ADB_WIFI_ENABLED)) {
            assertEquals(key, expected, Settings.Global.getInt(application.contentResolver, key, -1))
        }
    }

    private fun resetPreferences() {
        listOf("qt_settings_prefs", "usb_tile_service_state", "auto_revert_coordination").forEach { name ->
            application.getSharedPreferences(name, 0).edit().clear().commit()
        }
        AppStores::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
}
