package com.rbn.qtsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppNavigationContent
import com.rbn.qtsettings.app.navigation.AppRoute
import com.rbn.qtsettings.app.navigation.rememberAppNavigation
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val cleared = AtomicInteger()

    @Composable private fun NavigationHarness() {
        val nav = rememberAppNavigation()
        QuickTileSettingsTheme {
            Column {
                Button(onClick = { nav.selectArea(AppArea.Dns) }) { Text("Select DNS") }
                Button(onClick = { nav.selectArea(AppArea.Usb) }) { Text("Select USB") }
                Button(onClick = { nav.openTileArea(AppArea.Usb) }) { Text("USB tile") }
                Button(onClick = nav::back) { Text("Back") }
                AppNavigationContent(nav, Modifier.weight(1f)) { route ->
                    val owner = viewModel { EntryOwner(cleared) }
                    var draft by rememberSaveable { mutableStateOf("") }
                    Column {
                        Text(when (route) {
                            AppRoute.Dns -> "DNS root"
                            AppRoute.DnsProviders -> "DNS providers"
                            AppRoute.DnsAutomation -> "DNS automation"
                            AppRoute.DnsWifiTarget -> "Wi-Fi DNS target"
                            AppRoute.DnsMobileTarget -> "Mobile DNS target"
                            AppRoute.WifiRules -> "Wi-Fi rules"
                            is AppRoute.WifiRule -> "Wi-Fi rule ${route.ruleId}"
                            AppRoute.DnsRevert -> "DNS revert"
                            AppRoute.Usb -> "USB root"
                            AppRoute.UsbTile -> "USB tile settings"
                            AppRoute.UsbActions -> "USB actions"
                            AppRoute.UsbRevert -> "USB revert"
                            AppRoute.About -> "About"
                            is AppRoute.Settings -> "${route.origin} settings"
                            is AppRoute.Shortcuts -> "${route.origin} shortcuts"
                        })
                        Text("Owner ${owner.id}")
                        BasicTextField(draft, { draft = it }, Modifier.testTag("draft"))
                        Button(onClick = { nav.open(AppDestination.AppSettings) }) { Text("Settings") }
                    }
                }
            }
        }
    }

    private fun click(label: String) { compose.onNodeWithText(label).performClick() }
    private fun draft(value: String) { compose.onNodeWithTag("draft").performTextReplacement(value) }
    private fun assertDraft(value: String) { compose.onNodeWithTag("draft").assertTextEquals(value) }

    @Test fun bothAreaStacksAndEntryDraftsRestore_withoutSelectionHistoryOrDuplicateEntries() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { NavigationHarness() }
        draft("DNS root draft")
        click("Settings")
        draft("DNS form")
        click("Select USB")
        draft("USB root draft")
        click("Settings")
        draft("USB form")
        click("Settings") // Reopening the visible destination must not duplicate it.
        click("Select USB") // Reselecting the area must not reset its entry.
        assertDraft("USB form")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Usb settings").assertExists()
        assertDraft("USB form")
        click("Select DNS")
        assertDraft("DNS form")
        click("Back")
        assertDraft("DNS root draft")
        click("Select USB")
        assertDraft("USB form")
        click("Back")
        assertDraft("USB root draft")
        click("Back") // No synthetic DNS-selection history at a root.
        compose.onNodeWithText("USB root").assertExists()
    }

    @Test fun tileEntryResetsOnlyTargetStack_andPoppingClearsEntryOwner() {
        compose.setContent { NavigationHarness() }
        click("Settings")
        draft("Keep DNS form")
        click("Select USB")
        click("Settings")
        draft("Discard USB form")
        click("USB tile")
        compose.onNodeWithText("USB root").assertExists()
        assertClearedOwners(1)
        click("USB tile")
        compose.runOnIdle { assertEquals(1, cleared.get()) }
        click("Select DNS")
        assertDraft("Keep DNS form")
        click("Back")
        compose.onNodeWithText("DNS root").assertExists()
        assertClearedOwners(2)
    }

    private fun assertClearedOwners(expected: Int) {
        // Nav3 disposes a popped entry after its transition leaves composition.
        // Drain Compose work (including the animation clock) before checking onCleared;
        // a wall-clock poll can expire while a slow emulator is still rendering the pop.
        compose.runOnIdle { assertEquals(expected, cleared.get()) }
    }

    private class EntryOwner(private val cleared: AtomicInteger) : ViewModel() {
        val id = System.identityHashCode(this)
        override fun onCleared() { cleared.incrementAndGet() }
    }
}
