package com.rbn.qtsettings

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rbn.qtsettings.app.navigation.*
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.dns.domain.*
import com.rbn.qtsettings.feature.dns.presentation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DnsUiInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var activity: ComponentActivity
    private var lastAction: DnsSettingsAction? = null
    private var savedDelay: Int? = null
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))
    private val provider = DnsProvider("home", "Home DNS", DnsTarget.Hostname("dns.home.example.com"), false, false)

    @Composable private fun Harness(initial: AppDestination? = null, empty: Boolean = false, permission: Boolean = true) {
        activity = LocalActivity.current as ComponentActivity
        val nav = rememberAppNavigation()
        LaunchedEffect(Unit) { initial?.let(nav::open) }
        var state by remember { mutableStateOf(DnsSettingsState(
            DnsSettings(providers = if (empty) emptyList() else listOf(provider), enableAutoRevert = true),
            active = DnsTarget.Automatic, hasPermission = permission)) }
        val action: (DnsSettingsAction) -> Unit = { action ->
            lastAction = action
            when (action) {
                is DnsSettingsAction.RevertDelay -> { savedDelay = action.value; state = state.copy(settings = state.settings.copy(autoRevertDelaySeconds = action.value)) }
                is DnsSettingsAction.Selected -> state = state.copy(settings = state.settings.copy(providers = state.settings.providers.map { it.copy(selectedForCycle = action.value) }))
                else -> Unit
            }
        }
        QuickTileSettingsTheme {
            Column {
                Button(onClick = { nav.selectArea(AppArea.Dns) }) { Text("DNS area") }
                Button(onClick = { nav.selectArea(AppArea.Usb) }) { Text("USB area") }
                AppNavigationContent(nav, Modifier.weight(1f)) { route ->
                    when (route) {
                        AppRoute.Dns -> DnsSettingsScreen(state, action, nav::open)
                        AppRoute.DnsProviders -> DnsProvidersScreen(state, action, { true })
                        AppRoute.DnsRevert -> DnsAutoRevertScreen(state, action)
                        AppRoute.DnsAutomation -> Text("Automation detail")
                        else -> Text("USB root")
                    }
                }
            }
        }
    }

    @Test fun providerSelectionAndActivationAreSeparate_andMissingPermissionDisablesOnlyActivation() {
        compose.setContent { Harness(AppDestination.DnsProviders, permission = false) }
        compose.onNodeWithText(provider.name).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(DnsSettingsAction.Selected(provider.id, true), lastAction) }
        compose.onNodeWithContentDescription(context.getString(R.string.dns_entry_actions, provider.name, provider.target.value)).performClick()
        text(R.string.dns_set_active).assertIsNotEnabled()
        text(R.string.button_edit).assertIsEnabled()
    }

    @Test fun directProviderActionUsesHostname_withoutChangingCycleSelection() {
        compose.setContent { Harness(AppDestination.DnsProviders) }
        compose.onNodeWithContentDescription(context.getString(R.string.dns_entry_actions, provider.name, provider.target.value)).performScrollTo().performClick()
        text(R.string.dns_set_active).performClick()
        compose.runOnIdle { assertEquals(DnsSettingsAction.Activate(provider.target, provider.name), lastAction) }
        compose.onNodeWithText(provider.name).assertIsOff()
    }

    @Test fun dnsDetailsAndDelayDraftRestoreAcrossAreaSwitchAndPredictiveBack() {
        val restore = StateRestorationTester(compose)
        restore.setContent { Harness() }
        text(R.string.usb_menu_revert).performScrollTo().performClick()
        text(R.string.setting_auto_revert_delay).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("23")
        restore.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("23")
        text(android.R.string.cancel).performClick()
        assertNull(savedDelay)
        compose.onNodeWithText("USB area").performClick()
        compose.onNodeWithText("DNS area").performClick()
        text(R.string.setting_auto_revert_delay).assertIsDisplayed()
        compose.runOnIdle {
            activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT))
            activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(120f, 200f, .5f, BackEventCompat.EDGE_LEFT))
            activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }
        text(R.string.setting_auto_revert_delay).assertIsDisplayed()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        text(R.string.dns_menu_providers).assertIsDisplayed()
    }

    @Test fun delayRejectsNonNumbers_andRetainsLegacyZeroSemantics() {
        compose.setContent { Harness(AppDestination.DnsRevert) }
        text(R.string.setting_auto_revert_delay).performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("invalid")
        text(R.string.usb_apply).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("0")
        text(R.string.usb_apply).performClick()
        compose.runOnIdle { assertEquals(0, savedDelay) }
    }

    @Test fun emptyProvidersKeepSystemChoicesAndAddActionAvailable() {
        compose.setContent { Harness(AppDestination.DnsProviders, empty = true) }
        text(R.string.dns_providers_empty).performScrollTo().assertIsDisplayed()
        text(R.string.dns_add_custom_hostname_button).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(DnsSettingsAction.Edit(null), lastAction) }
    }
}
