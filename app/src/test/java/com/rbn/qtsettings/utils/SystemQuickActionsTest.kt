package com.rbn.qtsettings.utils

import android.Manifest
import android.app.Application
import android.provider.Settings
import com.rbn.qtsettings.utils.Constants.ADB_ENABLED
import com.rbn.qtsettings.utils.Constants.ADB_WIFI_ENABLED
import com.rbn.qtsettings.utils.Constants.DEVELOPMENT_SETTINGS_ENABLED
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SystemQuickActionsTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        Settings.Global.putString(application.contentResolver, PRIVATE_DNS_MODE, DNS_MODE_OFF)
        Settings.Global.putString(application.contentResolver, PRIVATE_DNS_SPECIFIER, null)
        Settings.Global.putInt(application.contentResolver, DEVELOPMENT_SETTINGS_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, ADB_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, ADB_WIFI_ENABLED, 0)
    }

    @After
    fun tearDown() {
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
    }

    @Test
    fun setActiveDns_withPermission_setsHostnameModeAndSpecifier() {
        val autoRevertGeneration = AutoRevertCoordinator.dnsGeneration(application)
        val result = SystemQuickActions.setActiveDns(application, "dns.example.com")

        assertEquals(SystemQuickActionResult.SUCCESS, result)
        assertEquals(
            autoRevertGeneration + 1L,
            AutoRevertCoordinator.dnsGeneration(application)
        )
        assertEquals(
            DNS_MODE_ON,
            Settings.Global.getString(application.contentResolver, PRIVATE_DNS_MODE)
        )
        assertEquals(
            "dns.example.com",
            Settings.Global.getString(application.contentResolver, PRIVATE_DNS_SPECIFIER)
        )
        assertEquals("dns.example.com", SystemQuickActions.getActiveDnsHostname(application))
    }

    @Test
    fun setActiveDns_withoutPermission_doesNotChangeSettings() {
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        val autoRevertGeneration = AutoRevertCoordinator.dnsGeneration(application)

        val result = SystemQuickActions.setActiveDns(application, "dns.example.com")

        assertEquals(SystemQuickActionResult.PERMISSION_MISSING, result)
        assertEquals(
            autoRevertGeneration,
            AutoRevertCoordinator.dnsGeneration(application)
        )
        assertEquals(
            DNS_MODE_OFF,
            Settings.Global.getString(application.contentResolver, PRIVATE_DNS_MODE)
        )
        assertNull(Settings.Global.getString(application.contentResolver, PRIVATE_DNS_SPECIFIER))
    }

    @Test
    fun setActiveDns_withBlankHostname_rejectsAction() {
        val result = SystemQuickActions.setActiveDns(application, "   ")

        assertEquals(SystemQuickActionResult.INVALID_DNS_HOSTNAME, result)
        assertNull(SystemQuickActions.getActiveDnsHostname(application))
    }

    @Test
    fun setActiveDns_withMalformedHostname_rejectsAction() {
        val result = SystemQuickActions.setActiveDns(application, "bad host!")

        assertEquals(SystemQuickActionResult.INVALID_DNS_HOSTNAME, result)
        assertEquals(
            DNS_MODE_OFF,
            Settings.Global.getString(application.contentResolver, PRIVATE_DNS_MODE)
        )
        assertNull(SystemQuickActions.getActiveDnsHostname(application))
    }

    @Test
    fun setDnsMode_withPermission_switchesBetweenOffAndAuto() {
        val initialGeneration = AutoRevertCoordinator.dnsGeneration(application)
        val autoResult = SystemQuickActions.setDnsMode(application, DNS_MODE_AUTO)

        assertEquals(SystemQuickActionResult.SUCCESS, autoResult)
        assertEquals(
            initialGeneration + 1L,
            AutoRevertCoordinator.dnsGeneration(application)
        )
        assertEquals(DNS_MODE_AUTO, SystemQuickActions.getActiveDnsMode(application))
        assertNull(SystemQuickActions.getActiveDnsHostname(application))

        val offResult = SystemQuickActions.setDnsMode(application, DNS_MODE_OFF)

        assertEquals(SystemQuickActionResult.SUCCESS, offResult)
        assertEquals(
            initialGeneration + 2L,
            AutoRevertCoordinator.dnsGeneration(application)
        )
        assertEquals(DNS_MODE_OFF, SystemQuickActions.getActiveDnsMode(application))
    }

    @Test
    fun setDnsMode_fromHostnameMode_keepsSpecifierForLaterReuse() {
        SystemQuickActions.setActiveDns(application, "dns.example.com")

        val result = SystemQuickActions.setDnsMode(application, DNS_MODE_OFF)

        assertEquals(SystemQuickActionResult.SUCCESS, result)
        assertEquals(DNS_MODE_OFF, SystemQuickActions.getActiveDnsMode(application))
        assertNull(SystemQuickActions.getActiveDnsHostname(application))
        assertEquals(
            "dns.example.com",
            Settings.Global.getString(application.contentResolver, PRIVATE_DNS_SPECIFIER)
        )
    }

    @Test
    fun setDnsMode_withoutPermission_doesNotChangeSettings() {
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)

        val result = SystemQuickActions.setDnsMode(application, DNS_MODE_AUTO)

        assertEquals(SystemQuickActionResult.PERMISSION_MISSING, result)
        assertEquals(DNS_MODE_OFF, SystemQuickActions.getActiveDnsMode(application))
    }

    @Test
    fun setDnsMode_withHostnameMode_rejectsAction() {
        val result = SystemQuickActions.setDnsMode(application, DNS_MODE_ON)

        assertEquals(SystemQuickActionResult.FAILED, result)
        assertEquals(DNS_MODE_OFF, SystemQuickActions.getActiveDnsMode(application))
    }

    @Test
    fun setUsbDebugging_withoutDeveloperOptions_rejectsAction() {
        val result = SystemQuickActions.setUsbDebuggingEnabled(
            context = application,
            enabled = true,
            toggleDeveloperOptions = false,
            toggleWirelessDebugging = false
        )

        assertEquals(SystemQuickActionResult.DEVELOPER_OPTIONS_DISABLED, result)
        assertFalse(SystemQuickActions.isUsbDebuggingEnabled(application))
    }

    @Test
    fun setUsbDebugging_withCompanionOptions_updatesAllConfiguredStates() {
        val initialGeneration = AutoRevertCoordinator.usbGeneration(application)
        val enabledResult = SystemQuickActions.setUsbDebuggingEnabled(
            context = application,
            enabled = true,
            toggleDeveloperOptions = true,
            toggleWirelessDebugging = true
        )

        assertEquals(SystemQuickActionResult.SUCCESS, enabledResult)
        assertEquals(
            initialGeneration + 1L,
            AutoRevertCoordinator.usbGeneration(application)
        )
        assertTrue(SystemQuickActions.isUsbDebuggingEnabled(application))
        assertEquals(1, Settings.Global.getInt(application.contentResolver, ADB_WIFI_ENABLED, 0))

        val disabledResult = SystemQuickActions.setUsbDebuggingEnabled(
            context = application,
            enabled = false,
            toggleDeveloperOptions = true,
            toggleWirelessDebugging = true
        )

        assertEquals(SystemQuickActionResult.SUCCESS, disabledResult)
        assertEquals(
            initialGeneration + 2L,
            AutoRevertCoordinator.usbGeneration(application)
        )
        assertFalse(SystemQuickActions.isUsbDebuggingEnabled(application))
        assertEquals(
            0,
            Settings.Global.getInt(application.contentResolver, DEVELOPMENT_SETTINGS_ENABLED, 1)
        )
        assertEquals(0, Settings.Global.getInt(application.contentResolver, ADB_WIFI_ENABLED, 1))
    }
}
