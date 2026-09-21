package com.rbn.qtsettings.platform.usb

import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.DebuggingSettingsProvider
import com.rbn.qtsettings.platform.permissions.PermissionUtils

import android.Manifest
import android.app.Application
import android.provider.Settings
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
class DebuggingSettingsReaderTest {
    private lateinit var application: Application
    private lateinit var provider: DebuggingSettingsProvider

    @Before fun setUp() {
        application = RuntimeEnvironment.getApplication()
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        provider = DebuggingSettingsProvider().apply { register(application) }
        Settings.Global.putInt(application.contentResolver, Constants.ADB_ENABLED, 0)
        Settings.Global.putInt(application.contentResolver, Constants.DEVELOPMENT_SETTINGS_ENABLED, 0)
    }

    @Test fun redactedReads_recoverBothFlags_withoutWritePermission() {
        provider.values[Constants.ADB_ENABLED] = "1"
        provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = "1"

        assertFalse(PermissionUtils.hasWriteSecureSettingsPermission(application))
        assertTrue(PermissionUtils.isDeveloperOptionsEnabled(application))
        assertTrue(AndroidUsbSystemGateway(application).read().active)
        assertEquals(setOf(Constants.ADB_ENABLED, Constants.DEVELOPMENT_SETTINGS_ENABLED),
            provider.requestedKeys.toSet())
    }

    @Test fun publicEnabledValues_doNotNeedInternalProtocol() {
        Settings.Global.putInt(application.contentResolver, Constants.ADB_ENABLED, 1)
        Settings.Global.putInt(application.contentResolver, Constants.DEVELOPMENT_SETTINGS_ENABLED, 1)
        provider.failure = UnsupportedOperationException()

        assertTrue(PermissionUtils.isDeveloperOptionsEnabled(application))
        assertTrue(AndroidUsbSystemGateway(application).read().active)
        assertTrue(provider.requestedKeys.isEmpty())
    }

    @Test fun disabledMissingAndMalformedValues_areNotTreatedAsEnabled() {
        for (value in listOf("0", "", "true", "2", "invalid", null)) {
            provider.values.clear()
            if (value != null) {
                provider.values[Constants.ADB_ENABLED] = value
                provider.values[Constants.DEVELOPMENT_SETTINGS_ENABLED] = value
            }
            assertFalse(DebuggingSettingsReader.isUsbDebuggingEnabled(application.contentResolver))
            assertFalse(PermissionUtils.isDeveloperOptionsEnabled(application))
        }
    }

    @Test fun unavailableProviderResponse_failsClosed() {
        provider.returnNull = true
        assertFalse(DebuggingSettingsReader.isUsbDebuggingEnabled(application.contentResolver))
        assertFalse(PermissionUtils.isDeveloperOptionsEnabled(application))
    }

    @Test fun rejectedOrUnsupportedCalls_failClosed() {
        for (failure in listOf(SecurityException(), UnsupportedOperationException(), IllegalArgumentException())) {
            provider.failure = failure
            assertFalse(DebuggingSettingsReader.isUsbDebuggingEnabled(application.contentResolver))
            assertFalse(PermissionUtils.isDeveloperOptionsEnabled(application))
        }
    }

    @Test fun fallbackIsReadAgain_afterAnExternalChange() {
        provider.values[Constants.ADB_ENABLED] = "1"
        assertTrue(DebuggingSettingsReader.isUsbDebuggingEnabled(application.contentResolver))
        provider.values[Constants.ADB_ENABLED] = "0"
        assertFalse(DebuggingSettingsReader.isUsbDebuggingEnabled(application.contentResolver))
    }

}
