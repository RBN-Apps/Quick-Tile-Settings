package com.rbn.qtsettings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.ui.composables.WifiNetworkRulesSection
import com.rbn.qtsettings.ui.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiNetworkRulesSectionInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun manualRuleEditor_savesSsidAndDefaultOffAction() {
        var savedSsid: String? = null
        var savedAction: String? = null
        setContent(
            onAddRule = { ssid, _, action, _ ->
                savedSsid = ssid
                savedAction = action
                true
            }
        )

        composeTestRule.onNodeWithTag("wifi_rule_add_manual").performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("Campus")
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save)).performClick()

        assertEquals("Campus", savedSsid)
        assertEquals(DNS_MODE_OFF, savedAction)
    }

    @Test
    fun manualRuleEditor_restoresOpenDialogAndInputAfterStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { RuleSection() }

        composeTestRule.onNodeWithTag("wifi_rule_add_manual").performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("Campus")

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_rule_add_title))
            .assertIsDisplayed()
        composeTestRule.onAllNodes(hasSetTextAction())[0].assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Campus"))
        )
    }

    @Test
    fun deleteRuleDialog_restoresAfterStateRestoration() {
        val rule = WifiDnsRule(
            id = "campus-rule",
            ssid = "Campus",
            actionMode = DNS_MODE_OFF
        )
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { RuleSection(rules = listOf(rule)) }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.wifi_rule_delete_description, "Campus")
        ).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(context.getString(R.string.wifi_rule_delete_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_rule_delete_message, "Campus")
        ).assertIsDisplayed()
    }

    @Test
    fun missingPermission_explainsThatManualEntryStillNeedsRuntimeAccess() {
        composeTestRule.setContent {
            RuleSection(
                wifiIdentityAccessState =
                    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_ssid_manual_entry_notice), substring = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("wifi_rule_add_manual").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_rule_add_title))
            .assertIsDisplayed()
    }

    @Test
    fun disabledLocationServices_accessActionOpensLocationSettings() {
        var settingsOpened = false
        composeTestRule.setContent {
            RuleSection(
                wifiIdentityAccessState = WifiIdentityAccessState.LOCATION_SERVICES_DISABLED,
                onOpenLocationSettings = { settingsOpened = true }
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.wifi_location_services_title)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("wifi_identity_access_action").performClick()

        assertEquals(true, settingsOpened)
    }

    private fun setContent(
        onAddRule: (String?, String?, String, String?) -> Boolean
    ) {
        composeTestRule.setContent {
            RuleSection(onAddRule = onAddRule)
        }
    }

    @Test
    fun currentNetwork_defaultsToSsidOnly() {
        var saved = false
        var savedBssid: String? = "unexpected"
        composeTestRule.setContent {
            RuleSection(
                currentNetwork = WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
                onAddRule = { ssid, bssid, _, _ ->
                    assertEquals("Campus", ssid)
                    savedBssid = bssid
                    saved = true
                    true
                }
            )
        }
        composeTestRule.onNodeWithTag("wifi_rule_add_current").performClick()
        composeTestRule.onAllNodes(hasSetTextAction())[1].assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save)).performClick()
        assertEquals(true, saved)
        assertNull(savedBssid)
    }

    @Test
    fun currentNetwork_canCopyDetectedBssidAfterStateRestoration() {
        var savedBssid: String? = null
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RuleSection(
                currentNetwork = WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
                onAddRule = { _, bssid, _, _ -> savedBssid = bssid; true }
            )
        }
        composeTestRule.onNodeWithTag("wifi_rule_add_current").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithTag("wifi_rule_use_detected_bssid").performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save)).performClick()
        assertEquals("AA:BB:CC:DD:EE:FF", savedBssid)
    }

    @Test
    fun blockedLocationPermission_offersAppSettings() {
        var requested = false
        composeTestRule.setContent {
            RuleSection(
                wifiIdentityAccessState = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED,
                onRequestPermission = { requested = true }
            )
        }
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_permission_open_settings)).performClick()
        assertEquals(true, requested)
    }

    @Test
    fun bootAccess_isOptionalAndOffersSettingsWhenMissing() {
        var opened = false
        composeTestRule.setContent {
            RuleSection(
                hasBackgroundLocationPermission = false,
                onOpenAppPermissionSettings = { opened = true }
            )
        }
        composeTestRule.onNodeWithTag("wifi_boot_permission_action").performClick()
        assertEquals(true, opened)
        composeTestRule.onNodeWithTag("wifi_rule_add_manual").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.wifi_rule_add_title)).assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun RuleSection(
        rules: List<WifiDnsRule> = emptyList(),
        onAddRule: (String?, String?, String, String?) -> Boolean = { _, _, _, _ -> true },
        wifiIdentityAccessState: WifiIdentityAccessState = WifiIdentityAccessState.AVAILABLE,
        onOpenLocationSettings: () -> Unit = {},
        currentNetwork: WifiNetworkIdentity? = null,
        onRequestPermission: () -> Unit = {},
        hasBackgroundLocationPermission: Boolean = true,
        onOpenAppPermissionSettings: () -> Unit = {}
    ) {
        QuickTileSettingsTheme {
            WifiNetworkRulesSection(
                enabled = true,
                rules = rules,
                dnsHostnames = emptyList(),
                currentNetwork = currentNetwork,
                scannedNetworks = emptyList(),
                knownNetworks = emptyList(),
                isLoadingNetworks = false,
                wifiIdentityAccessState = wifiIdentityAccessState,
                onEnabledChange = {},
                onAddRule = onAddRule,
                onUpdateRule = { _, _, _, _, _ -> false },
                onDeleteRule = {},
                onRefreshNetworks = {},
                onRequestPermission = onRequestPermission,
                onOpenLocationSettings = onOpenLocationSettings,
                hasBackgroundLocationPermission = hasBackgroundLocationPermission,
                onOpenAppPermissionSettings = onOpenAppPermissionSettings
            )
        }
    }
}
