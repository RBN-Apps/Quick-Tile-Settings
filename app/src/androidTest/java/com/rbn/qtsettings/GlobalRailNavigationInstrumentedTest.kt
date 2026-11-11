package com.rbn.qtsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rbn.qtsettings.app.navigation.*
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GlobalRailNavigationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: AppNavigation
    private fun show(): StateRestorationTester = StateRestorationTester(compose).also { restore ->
        restore.setContent {
            nav = rememberAppNavigation()
            QuickTileSettingsTheme {
                AppNavigationContent(nav) { route ->
                    var draft by rememberSaveable { mutableStateOf("") }
                    Column {
                        Text(route.toString())
                        BasicTextField(draft, { draft = it }, Modifier.testTag("draft"))
                    }
                }
            }
        }
    }
    private fun act(action: AppNavigation.() -> Unit) { compose.runOnIdle { nav.action() } }
    private fun draft(value: String) { compose.onNodeWithTag("draft").performTextReplacement(value) }
    private fun checkDraft(value: String) { compose.onNodeWithTag("draft").assertTextEquals(value) }
    private fun selected(target: AppRailTarget) { compose.runOnIdle { assertEquals(target, nav.selectedTarget) } }

    @Test fun globalRailReplacesHistory_preservesCallerDraftAndRestoresCrossLinks() {
        val restore = show()
        act { open(AppDestination.DnsProviders) }
        draft("DNS provider draft")
        act { selectArea(AppArea.Usb); open(AppDestination.UsbActions) }
        draft("USB actions draft")
        act { selectTarget(AppRailTarget.Settings) }
        draft("same settings entry")
        act { selectTarget(AppRailTarget.Settings) }
        checkDraft("same settings entry")
        act { selectTarget(AppRailTarget.Shortcuts); selectTarget(AppRailTarget.About) }
        selected(AppRailTarget.About)
        draft("about scroll")
        act { open(AppDestination.Permissions) }
        draft("permission scroll")
        act { open(AppDestination.AdbInstructions) }
        selected(AppRailTarget.Permissions)
        restore.emulateSavedInstanceStateRestore()
        selected(AppRailTarget.Permissions)
        act { back() }; checkDraft("permission scroll")
        act { back() }; checkDraft("about scroll")
        act { back() }; checkDraft("USB actions draft")
        selected(AppRailTarget.Usb)
        act { selectArea(AppArea.Dns) }; checkDraft("DNS provider draft")
    }

    @Test fun railAreaLeavesGlobalPath_andExternalEntryOnlyResetsItsFeature() {
        show()
        act { open(AppDestination.DnsProviders) }; draft("preserved DNS")
        act { selectTarget(AppRailTarget.About); open(AppDestination.Permissions) }
        act { selectTarget(AppRailTarget.Dns) }; checkDraft("preserved DNS")
        act { selectArea(AppArea.Usb); open(AppDestination.UsbActions) }; draft("USB draft")
        act { selectTarget(AppRailTarget.Permissions); open(AppDestination.AdbInstructions) }
        act { openTileArea(AppArea.Usb) }
        selected(AppRailTarget.Usb)
        compose.onNodeWithText("Usb").assertExists()
        act { selectArea(AppArea.Dns) }; checkDraft("preserved DNS")
    }

    @Test fun optionalAutomationHasItsOwnEntry_whenDnsAutomationIsTheCaller() {
        show()
        act { open(AppDestination.DnsAutomation) }; draft("caller automation")
        act { open(AppDestination.Permissions); openPermissionAutomation() }
        draft("help automation")
        act { back(); back() }; checkDraft("caller automation")
    }

    @Test fun contextualShortcutsKeepFocus_globalRailEntryStartsWithoutFocus() {
        show()
        act { selectArea(AppArea.Usb); openAreaShortcuts() }
        compose.onNodeWithText(AppRoute.Shortcuts(AppArea.Usb, true).toString()).assertExists()
        selected(AppRailTarget.Shortcuts)
        act { selectTarget(AppRailTarget.Shortcuts) }
        compose.onNodeWithText(AppRoute.Shortcuts(AppArea.Usb).toString()).assertExists()
        act { back() }
        compose.onNodeWithText("Usb").assertExists()
    }
}
