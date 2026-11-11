package com.rbn.qtsettings.platform.usb

import android.Manifest
import android.app.Application
import android.provider.Settings
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.feature.usb.domain.UsbSystemState
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.DebuggingSettingsProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidUsbSystemGatewayTest {
    private lateinit var application: Application
    private lateinit var provider: DebuggingSettingsProvider
    private lateinit var actions: UsbActions

    @Before fun setUp() {
        application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        provider = DebuggingSettingsProvider().apply { register(application) }
        Settings.Global.putInt(application.contentResolver, Constants.ADB_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, Constants.DEVELOPMENT_SETTINGS_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, Constants.ADB_WIFI_ENABLED, 1)
        provider.values[Constants.ADB_ENABLED] = "1"
        provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "1"
        actions = UsbActions(AndroidUsbSystemGateway(application))
    }

    @Test fun cycleUsesActualState_andRestoresTheCorrectSnapshot() {
        val settings = UsbSettings(alsoHideDevOptions = true, alsoDisableWirelessDebugging = true)
        val cycle = actions.cycle(settings)

        assertEquals(UsbActionResult.Success, cycle.result)
        assertEquals(UsbSystemState(true, true, true), cycle.previous)
        assertWrittenState(0)

        provider.values[Constants.ADB_ENABLED] = "0"
        provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "0"
        assertEquals(UsbActionResult.Success, actions.restore(cycle.previous!!, cycle.generation, settings))
        assertWrittenState(1)
    }

    @Test fun cycleCanTurnBackOn_whenActualFlagsAreOff() {
        provider.values[Constants.ADB_ENABLED] = "0"
        provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "0"
        assertEquals(UsbActionResult.Success,
            actions.cycle(UsbSettings(alsoHideDevOptions = true, alsoDisableWirelessDebugging = true)).result)
        assertWrittenState(1)
    }

    @Test fun actionWithoutDeveloperOptionsCompanion_isNotBlockedByRedaction() {
        provider.values[Constants.ADB_ENABLED] = "0"
        assertEquals(UsbActionResult.Success, actions.set(true, false, false))
        assertEquals(1, Settings.Global.getInt(application.contentResolver, Constants.ADB_ENABLED, 0))
    }

    @Test fun wirelessDebuggingStillUsesThePublicRead() {
        provider.values[Constants.ADB_WIFI_ENABLED] = "0"
        assertTrue(actions.read().wireless)
        assertFalse(provider.requestedKeys.contains(Constants.ADB_WIFI_ENABLED))
        Settings.Global.putInt(application.contentResolver, Constants.ADB_WIFI_ENABLED, 0)
        assertFalse(actions.read().wireless)
    }

    private fun assertWrittenState(expected: Int) {
        for (key in listOf(Constants.ADB_ENABLED, Constants.DEVELOPMENT_SETTINGS_ENABLED, Constants.ADB_WIFI_ENABLED)) {
            assertEquals(key, expected, Settings.Global.getInt(application.contentResolver, key, -1))
        }
    }
}
