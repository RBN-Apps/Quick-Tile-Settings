package com.rbn.qtsettings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.ui.composables.permission.AdbInstructionDialog
import com.rbn.qtsettings.ui.composables.permission.PermissionGrantDialog
import com.rbn.qtsettings.ui.composables.settings.NotificationPermissionSettingsDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsDialogsInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

    @Test fun adbCommands_copyExactValues_atLargeFont_withCloseAlwaysVisible() {
        val copied = mutableListOf<String>()
        var dismissed = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                QuickTileSettingsTheme { AdbInstructionDialog({ dismissed++ }, copied::add) }
            }
        }
        text(R.string.dialog_close).assertIsDisplayed()
        text(R.string.button_copy_grant_command).performScrollTo().performClick()
        text(R.string.button_copy_install_command).performScrollTo().performClick()
        text(R.string.dialog_close).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf("adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS",
                context.getString(R.string.adb_command_install_g)), copied)
            assertEquals(1, dismissed)
        }
    }

    @Test fun permissionMethods_availableActions_dispatchOnlyTheirCallback() {
        var root = 0
        var shizuku = 0
        var adb = 0
        var developer = 0
        compose.setContent {
            QuickTileSettingsTheme {
                PermissionGrantDialog({}, { developer++ }, { adb++ }, { shizuku++ }, { root++ },
                    { error("Already authorized Shizuku must not request permission again") }, true, true, true, true)
            }
        }
        text(R.string.button_grant_with_root).performScrollTo().performClick()
        text(R.string.button_grant_with_shizuku).performScrollTo().performClick()
        text(R.string.button_show_adb_instructions).performScrollTo().performClick()
        text(R.string.button_open_developer_options).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1), listOf(root, shizuku, adb, developer)) }
    }

    @Test fun notificationSettings_largeFont_keepsBothActions_andDoesNotOpenSettingsOnDismiss() {
        var opened = 0
        var continued = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                QuickTileSettingsTheme { NotificationPermissionSettingsDialog({ opened++ }, { continued++ }) }
            }
        }
        text(R.string.notification_permission_settings_button).assertIsDisplayed()
        text(R.string.notification_permission_continue_button).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, opened); assertEquals(1, continued) }
        text(R.string.notification_permission_settings_button).performClick()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, continued) }
    }
}
