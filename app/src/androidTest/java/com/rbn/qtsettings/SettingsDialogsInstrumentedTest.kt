package com.rbn.qtsettings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.permissions.presentation.*
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.platform.permissions.adbPermissionGrantCommand
import com.rbn.qtsettings.feature.settings.presentation.NotificationPermissionSettingsDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsDialogsInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))
    // The status row speaks as one node, so its state is only reachable as a description.
    private fun spoken(id: Int) =
        compose.onNodeWithContentDescription(context.getString(id), substring = true)

    @Test fun adbCommands_copyExactValues_atLargeFont() {
        val copied = mutableListOf<String>()
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                QuickTileSettingsTheme { AdbInstructionsScreen(adbPermissionGrantCommand(context.packageName), copied::add) }
            }
        }
        text(R.string.button_copy_grant_command).performScrollTo().performClick()
        text(R.string.button_copy_install_command).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS",
                context.getString(R.string.adb_command_install_g)), copied)
        }
    }

    @Test fun permissionMethods_availableActions_dispatchOnlyTheirCallback() {
        var root = 0
        var shizuku = 0
        var adb = 0
        var developer = 0
        compose.setContent {
            QuickTileSettingsTheme {
                PermissionSettingsScreen(PermissionSnapshot(shizukuAvailable = true, shizukuPermissionGranted = true, rooted = true, developerOptionsEnabled = true), true, PermissionOperation(), null, {}, { adb++ }, { developer++ },
                    { error("Already authorized Shizuku must not request permission again") }, { shizuku++ }, { root++ }, {})
            }
        }
        text(R.string.button_grant_with_root).performScrollTo().performClick()
        text(R.string.button_grant_with_shizuku).performScrollTo().performClick()
        text(R.string.button_show_adb_instructions).performScrollTo().performClick()
        text(R.string.button_open_developer_options).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1), listOf(root, shizuku, adb, developer)) }
    }

    @Test fun loadingFailureBusyAndGrantedStates_disablePrivilegedActions() {
        val operation = androidx.compose.runtime.mutableStateOf(PermissionOperation(checking = true))
        val granted = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            QuickTileSettingsTheme {
                PermissionSettingsScreen(PermissionSnapshot(hasWriteSecureSettings = granted.value,
                    shizukuAvailable = true, shizukuPermissionGranted = true, rooted = true),
                    true, operation.value, null, {}, {}, {}, {}, {}, {}, {})
            }
        }
        for (state in listOf(PermissionOperation(checking = true), PermissionOperation(checkFailed = true),
            PermissionOperation(granting = true))) {
            compose.runOnIdle { operation.value = state }
            text(R.string.button_grant_with_root).performScrollTo().assertIsNotEnabled()
            text(R.string.button_grant_with_shizuku).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { operation.value = PermissionOperation(); granted.value = true }
        text(R.string.button_grant_with_root).performScrollTo().assertIsNotEnabled()
        text(R.string.button_grant_with_shizuku).performScrollTo().assertIsNotEnabled()
        spoken(R.string.permission_is_granted).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { granted.value = false }
        text(R.string.button_grant_with_root).performScrollTo().assertIsEnabled()
        text(R.string.button_grant_with_shizuku).performScrollTo().assertIsEnabled()
    }

    @Test fun setupStatusRow_carriesTheCheckAgainAction_andStandsDownWhileBusy() {
        var refreshed = 0
        val operation = androidx.compose.runtime.mutableStateOf(PermissionOperation())
        compose.setContent {
            QuickTileSettingsTheme {
                PermissionSettingsScreen(PermissionSnapshot(), true, operation.value, null,
                    { refreshed++ }, {}, {}, {}, {}, {}, {})
            }
        }
        spoken(R.string.permission_not_setup).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refreshed); operation.value = PermissionOperation(checking = true) }
        spoken(R.string.permission_checking).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, refreshed) }
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
