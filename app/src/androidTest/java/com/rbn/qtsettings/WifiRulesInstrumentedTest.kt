package com.rbn.qtsettings

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.presentation.AutomationUiActions
import com.rbn.qtsettings.feature.automation.presentation.AutomationUiState
import com.rbn.qtsettings.feature.automation.presentation.WifiRuleEditorRoot
import com.rbn.qtsettings.feature.automation.presentation.WifiRulesScreen
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Rule overview and editor pages; storage callbacks are recorded, nothing is persisted. */
@RunWith(AndroidJUnit4::class)
class WifiRulesInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var backDispatcher: androidx.activity.OnBackPressedDispatcher

    private fun string(id: Int, vararg args: Any) = context.getString(id, *args)
    private val providers = listOf(DnsHostnameEntry("provider", "Example DNS", "dns.example.com"))
    private val campus = WifiDnsRule("campus", "Campus", actionMode = DNS_MODE_OFF)

    // ---- Rules overview ----

    @Test
    fun rulesOverview_listsRulesWithTargetsAndOpensEditors() {
        var added = 0
        var edited: String? = null
        val rules = listOf(campus, WifiDnsRule("home", "Home", "AA:BB:CC:DD:EE:FF", DNS_MODE_ON, "dns.example.com"))
        compose.setContent { Rules(state(rules = rules), onAddRule = { added++ }, onEditRule = { edited = it }) }

        compose.onNodeWithText("Campus").assertIsDisplayed()
        compose.onNodeWithText("AA:BB:CC:DD:EE:FF").assertIsDisplayed()
        compose.onNodeWithText("Example DNS").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.wifi_rules_fallback_status, string(R.string.dns_mode_auto_label)))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_home").performScrollTo().performClick()
        assertEquals("home", edited)
        compose.onNodeWithTag("wifi_rule_add").performScrollTo().performClick()
        assertEquals(1, added)
    }

    @Test
    fun rulesOverview_disabledRulesKeepListVisibleButInactive() {
        var enabledChange: Boolean? = null
        compose.setContent { Rules(state(rules = listOf(campus), rulesEnabled = false), onEnabledChange = { enabledChange = it }) }

        compose.onNodeWithText("Campus").assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_campus").assertIsNotEnabled()
        compose.onNodeWithTag("wifi_rule_add").assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.wifi_rules_empty_guidance)).assertDoesNotExist()
        compose.onNodeWithTag("wifi_identity_access_action").assertDoesNotExist()
        compose.onNodeWithTag("wifi_network_rules_toggle").performClick()
        assertEquals(true, enabledChange)
    }

    @Test
    fun rulesOverview_requiresNetworkSwitchingBeforeEnabling() {
        compose.setContent { Rules(state(networkDetection = false)) }

        compose.onNodeWithTag("wifi_network_rules_toggle").assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.automation_rules_requires_network)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.wifi_rules_empty_guidance)).assertIsDisplayed()
    }

    @Test
    fun rulesOverview_missingPermission_explainsManualEntryAndRequestsAccess() {
        var requested = 0
        compose.setContent {
            Rules(state(access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED), onRequestPermission = { requested++ })
        }

        compose.onNodeWithText(string(R.string.wifi_ssid_permission_title)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.wifi_ssid_manual_entry_notice)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_identity_access_docs").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_identity_access_action").performScrollTo().performClick()
        assertEquals(1, requested)
        compose.onNodeWithTag("wifi_rule_add").performScrollTo().assertIsEnabled()
    }

    @Test
    fun rulesOverview_blockedPermission_offersAppSettings() {
        var requested = 0
        compose.setContent {
            Rules(state(access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED), onRequestPermission = { requested++ })
        }
        compose.onNodeWithText(string(R.string.wifi_permission_blocked_description)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_identity_access_action").performScrollTo().performClick()
        assertEquals(1, requested)
    }

    @Test
    fun rulesOverview_disabledLocationServices_opensLocationSettings() {
        var opened = 0
        compose.setContent {
            Rules(state(access = WifiIdentityAccessState.LOCATION_SERVICES_DISABLED), onOpenLocationSettings = { opened++ })
        }
        compose.onNodeWithText(string(R.string.wifi_location_services_title)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_identity_access_action").performScrollTo().performClick()
        assertEquals(1, opened)
    }

    @Test
    fun rulesOverview_bootAccessIsOptionalAndOffersSettingsWhenMissing() {
        var opened = 0
        compose.setContent { Rules(state(backgroundLocation = false), onOpenAppPermissionSettings = { opened++ }) }
        compose.onNodeWithTag("wifi_boot_permission_action").performScrollTo().performClick()
        assertEquals(1, opened)
        compose.onNodeWithText(string(R.string.wifi_boot_ready)).assertDoesNotExist()
    }

    @Test
    fun rulesOverview_reportsReadyBootAccess() {
        compose.setContent { Rules(state()) }
        compose.onNodeWithText(string(R.string.wifi_boot_ready)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_boot_permission_action").assertDoesNotExist()
    }

    // ---- Rule editor ----

    @Test
    fun editor_manualRuleSavesSsidWithDefaultOffAndLeavesPage() {
        val actions = RecordingActions()
        var back = 0
        compose.setContent { Editor(null, state(), actions, onBack = { back++ }) }

        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Campus")
        saveRule()

        assertEquals(listOf(Saved(null, "Campus", null, DNS_MODE_OFF, null)), actions.saved)
        assertEquals(1, back)
        compose.onNodeWithTag("wifi_rule_delete").assertDoesNotExist()
    }

    @Test
    fun editor_invalidRuleKeepsErrorAndCanBeCorrected() {
        val actions = RecordingActions(accept = { it.ssid == "Campus" })
        var back = 0
        compose.setContent { Editor(null, state(), actions, onBack = { back++ }) }

        saveRule()
        compose.onNodeWithText(string(R.string.wifi_rule_invalid_or_duplicate)).performScrollTo().assertIsDisplayed()
        assertEquals(0, back)
        compose.onNodeWithTag("wifi_rule_ssid").performScrollTo().performTextInput("Campus")
        compose.onNodeWithText(string(R.string.wifi_rule_invalid_or_duplicate)).assertDoesNotExist()
        saveRule()
        assertEquals(1, actions.saved.size)
        assertEquals(1, back)
    }

    @Test
    fun editor_currentNetworkCopiesSsidOnlyAndDetectedBssidOnRequest() {
        val actions = RecordingActions()
        compose.setContent {
            Editor(null, state(current = WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF")), actions)
        }

        compose.onNodeWithText("Campus").performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Campus"))
        compose.onNodeWithTag("wifi_rule_bssid").assert(editableTextEquals(""))
        saveRule()
        assertNull(actions.saved.single().bssid)

        compose.onNodeWithTag("wifi_rule_previous").performClick()
        compose.onNodeWithTag("wifi_rule_use_detected_bssid").performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_bssid").assert(editableTextEquals("AA:BB:CC:DD:EE:FF"))
        compose.onNodeWithTag("wifi_rule_use_detected_bssid").assertDoesNotExist()
        saveRule()
        assertEquals("AA:BB:CC:DD:EE:FF", actions.saved.last().bssid)
    }

    @Test
    fun editor_targetChoiceIsOnlyStoredOnSave() {
        val actions = RecordingActions()
        compose.setContent { Editor(null, state(), actions) }

        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Campus")
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithText(string(R.string.wifi_rule_action_default_summary, string(R.string.dns_mode_auto_label)))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("automation_target_provider").performScrollTo().performClick()
        assertTrue(actions.saved.isEmpty())
        saveRule()
        assertEquals(Saved(null, "Campus", null, DNS_MODE_ON, "dns.example.com"), actions.saved.single())
    }

    @Test
    fun editor_existingRuleUpdatesInPlaceAndDeletesOnlyAfterConfirmation() {
        val actions = RecordingActions()
        var back = 0
        compose.setContent { Editor("campus", state(rules = listOf(campus)), actions, onBack = { back++ }) }

        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Campus"))
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("automation_target_default").performScrollTo().performClick()
        saveRule()
        assertEquals(Saved("campus", "Campus", null, WIFI_DNS_ACTION_DEFAULT, null), actions.saved.single())
        assertEquals(1, back)

        compose.onNodeWithTag("wifi_rule_delete").performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.wifi_rule_delete_message, "Campus")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.dialog_cancel)).performClick()
        assertTrue(actions.deleted.isEmpty())
        compose.onNodeWithTag("wifi_rule_delete").performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.button_delete)).performClick()
        assertEquals(listOf("campus"), actions.deleted)
        assertEquals(2, back)
    }

    @Test
    fun editor_suggestionsShowLoadingEmptyAndAccessStates() {
        var loading by mutableStateOf(true)
        var access by mutableStateOf(WifiIdentityAccessState.AVAILABLE)
        val actions = RecordingActions()
        compose.setContent { Editor(null, state(loading = loading, access = access), actions) }

        compose.onNodeWithText(string(R.string.wifi_network_scanning)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_network_refresh").assertIsNotEnabled()
        compose.runOnIdle { loading = false }
        compose.onNodeWithText(string(R.string.wifi_network_none_found)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_network_refresh").performScrollTo().assertIsEnabled().performClick()
        assertEquals(2, actions.refreshed) // once on open, once on demand
        compose.runOnIdle { access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED }
        compose.onNodeWithText(string(R.string.automation_picker_access)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_network_refresh").assertIsNotEnabled()
        compose.onNodeWithTag("wifi_identity_access_action").performScrollTo().performClick()
        assertEquals(1, actions.permissionRequests)
        compose.onNodeWithTag("wifi_rule_ssid").performScrollTo().assertIsEnabled()
    }

    @Test
    fun editor_recentNetworkSeedsRuleWithoutLocationAccess() {
        val actions = RecordingActions()
        compose.setContent {
            Editor(null, state(access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED,
                known = listOf(KnownWifiNetwork("Recent network", "AA:BB:CC:DD:EE:FF"))), actions)
        }
        assertEquals(0, actions.refreshed)
        compose.onNodeWithText("Recent network").performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Recent network"))
        compose.onNodeWithTag("wifi_rule_bssid").assert(editableTextEquals(""))
        compose.onNodeWithTag("wifi_rule_use_detected_bssid").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editor_nearbyAndHiddenNetworksAreListedOnce() {
        val actions = RecordingActions()
        val shared = WifiNetworkIdentity("Nearby network", "AA:BB:CC:DD:EE:FF")
        compose.setContent {
            Editor(null, state(current = shared, scanned = listOf(shared, WifiNetworkIdentity(null, "11:22:33:44:55:66")),
                known = listOf(KnownWifiNetwork("Nearby network", "AA:BB:CC:DD:EE:FF"))), actions)
        }
        assertEquals(1, compose.onAllNodesWithText("Nearby network").fetchSemanticsNodes().size)
        compose.onNodeWithText(string(R.string.wifi_hidden_network)).performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals(""))
        compose.onNodeWithTag("wifi_rule_bssid").assert(editableTextEquals("11:22:33:44:55:66"))
    }

    @Test
    fun editor_largeTextKeepsSaveVisibleWhileErrorIsShown() {
        val actions = RecordingActions(accept = { false })
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.8f)) {
                Editor(null, state(), actions)
            }
        }
        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Campus")
        compose.onNodeWithTag("wifi_rule_next").assertIsDisplayed().performClick()
        compose.onNodeWithTag("wifi_rule_save").assertIsDisplayed().performClick()
        compose.onNodeWithText(string(R.string.wifi_rule_invalid_or_duplicate)).assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_next").assertIsDisplayed()
    }

    @Test
    fun editor_searchesBothStepsAndGoingBackPreservesSelection() {
        val actions = RecordingActions()
        compose.setContent { Editor(null, state(scanned = listOf(
            WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
            WifiNetworkIdentity("Home", "11:22:33:44:55:66"))), actions) }
        compose.onNodeWithTag("wifi_rule_network_search").performScrollTo().performTextInput("aa:bb")
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Campus").performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assertDoesNotExist()
        assertTrue(actions.saved.isEmpty())
        compose.onNodeWithTag("wifi_rule_target_search").performTextInput("DNS.EXAMPLE")
        compose.onNodeWithTag("automation_target_off").assertDoesNotExist()
        compose.onNodeWithTag("automation_target_provider").performScrollTo().performClick()
        compose.onNodeWithTag("wifi_rule_previous").performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Campus"))
        compose.onNodeWithTag("wifi_rule_network_search").performScrollTo().assert(editableTextEquals("aa:bb"))
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("wifi_rule_target_search").assert(editableTextEquals("DNS.EXAMPLE"))
        compose.onNodeWithTag("automation_target_provider").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithTag("wifi_rule_save").performClick()
        assertEquals(Saved(null, "Campus", null, DNS_MODE_ON, "dns.example.com"), actions.saved.single())
    }

    @Test
    fun editor_searchNoResultsCanBeClearedWithoutLosingDraft() {
        compose.setContent { Editor(null, state(scanned = listOf(WifiNetworkIdentity("Campus", null))), RecordingActions()) }
        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Manual")
        compose.onNodeWithTag("wifi_rule_network_search").performScrollTo().performTextInput("missing")
        compose.onNodeWithText(string(R.string.wifi_rule_search_empty)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_network_search").performScrollTo().performTextReplacement("camp")
        compose.onNodeWithText("Campus").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("wifi_rule_target_search").performTextInput("missing")
        compose.onNodeWithText(string(R.string.wifi_rule_search_empty)).assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_target_search").performTextReplacement("")
        compose.onNodeWithTag("automation_target_off").assertIsDisplayed()
        compose.onNodeWithTag("wifi_rule_previous").performClick()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Manual"))
    }

    @Test
    fun editor_systemBackReturnsToNetworkWithoutSaving() {
        val actions = RecordingActions()
        compose.setContent { Editor(null, state(), actions) }
        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Campus")
        compose.onNodeWithTag("wifi_rule_next").performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Campus"))
        compose.onNodeWithTag("wifi_rule_next").assertIsDisplayed()
        assertTrue(actions.saved.isEmpty())
    }

    @Test
    fun editor_predictiveBackBothEdgesPreviewCancelAndCommitPreserveDraft() {
        val actions = RecordingActions()
        compose.setContent { Editor(null, state(), actions) }
        compose.onNodeWithTag("wifi_rule_ssid").performTextInput("Campus")
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("automation_target_provider").performScrollTo().performClick()
        fun x() = compose.onNodeWithTag("wifi_rule_step_1").fetchSemanticsNode().positionInRoot.x
        for (edge in listOf(androidx.activity.BackEventCompat.EDGE_LEFT, androidx.activity.BackEventCompat.EDGE_RIGHT)) {
            val resting = x()
            compose.runOnIdle { backDispatcher.dispatchOnBackStarted(androidx.activity.BackEventCompat(0f, 0f, 0f, edge)) }
            compose.runOnIdle { backDispatcher.dispatchOnBackProgressed(androidx.activity.BackEventCompat(0f, 0f, .2f, edge)) }
            val early = x()
            compose.runOnIdle { backDispatcher.dispatchOnBackProgressed(androidx.activity.BackEventCompat(0f, 0f, .65f, edge)) }
            assertTrue("Page follows the gesture to the right", x() > early + 10f)
            compose.onNodeWithTag("wifi_rule_step_0").assertExists()
            compose.runOnIdle { backDispatcher.dispatchOnBackCancelled() }
            assertEquals(resting, x(), 1f)
            compose.onNodeWithTag("automation_target_provider").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        }
        compose.runOnIdle { backDispatcher.dispatchOnBackStarted(androidx.activity.BackEventCompat(0f, 0f, 0f, 0)) }
        compose.runOnIdle { backDispatcher.dispatchOnBackProgressed(androidx.activity.BackEventCompat(0f, 0f, .8f, 0)) }
        compose.runOnIdle { backDispatcher.onBackPressed() }
        compose.onNodeWithTag("wifi_rule_step_1").assertDoesNotExist()
        compose.onNodeWithTag("wifi_rule_ssid").assert(editableTextEquals("Campus"))
        assertTrue(actions.saved.isEmpty())
        compose.onNodeWithTag("wifi_rule_next").performClick()
        compose.onNodeWithTag("automation_target_provider").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    // ---- Helpers ----
    private fun saveRule() {
        if (compose.onAllNodes(androidx.compose.ui.test.hasTestTag("wifi_rule_next")).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("wifi_rule_next").performClick()
        }
        if (compose.onAllNodes(androidx.compose.ui.test.hasTestTag("wifi_rule_save")).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("wifi_rule_save").performClick()
        }
    }


    private fun editableTextEquals(expected: String) =
        SemanticsMatcher("Editable text equals '$expected'") {
            it.config[SemanticsProperties.EditableText].text == expected
        }

    private fun state(
        rules: List<WifiDnsRule> = emptyList(),
        rulesEnabled: Boolean = true,
        networkDetection: Boolean = true,
        access: WifiIdentityAccessState = WifiIdentityAccessState.AVAILABLE,
        backgroundLocation: Boolean = true,
        current: WifiNetworkIdentity? = null,
        scanned: List<WifiNetworkIdentity> = emptyList(),
        known: List<KnownWifiNetwork> = emptyList(),
        loading: Boolean = false
    ) = AutomationUiState(
        vpnDetectionEnabled = false, vpnDetectionMode = BACKGROUND_DETECTION,
        networkTypeDetectionEnabled = networkDetection, networkTypeDetectionMode = BACKGROUND_DETECTION,
        dnsStateOnWifi = DNS_MODE_AUTO, dnsHostnameOnWifi = null,
        wifiNetworkRulesEnabled = rulesEnabled, wifiNetworkRules = rules,
        knownWifiNetworks = known, currentWifiNetwork = current, scannedWifiNetworks = scanned,
        isWifiResultsLoading = loading, wifiIdentityAccessState = access,
        hasBackgroundLocationPermission = backgroundLocation,
        dnsStateOnMobile = DNS_MODE_OFF, dnsHostnameOnMobile = null
    )

    @Composable
    private fun Rules(
        state: AutomationUiState,
        onEnabledChange: (Boolean) -> Unit = {},
        onAddRule: () -> Unit = {},
        onEditRule: (String) -> Unit = {},
        onRequestPermission: () -> Unit = {},
        onOpenLocationSettings: () -> Unit = {},
        onOpenAppPermissionSettings: () -> Unit = {}
    ) {
        QuickTileSettingsTheme {
            WifiRulesScreen(state, providers, onEnabledChange, onAddRule, onEditRule, onRequestPermission,
                onOpenLocationSettings, onOpenAppPermissionSettings, Modifier.padding(16.dp))
        }
    }

    @Composable
    private fun Editor(ruleId: String?, state: AutomationUiState, actions: RecordingActions, onBack: () -> Unit = {}) {
        backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        QuickTileSettingsTheme { WifiRuleEditorRoot(ruleId, state, actions, providers, {}, onBack) }
    }

    private data class Saved(val id: String?, val ssid: String?, val bssid: String?, val action: String, val hostname: String?)

    private class RecordingActions(private val accept: (Saved) -> Boolean = { true }) : AutomationUiActions {
        val saved = mutableListOf<Saved>()
        val deleted = mutableListOf<String>()
        var refreshed = 0
        var permissionRequests = 0
        override fun addWifiNetworkRule(ssid: String?, bssid: String?, actionMode: String, dnsHostname: String?) =
            Saved(null, ssid, bssid, actionMode, dnsHostname).let { saved += it; accept(it) }
        override fun updateWifiNetworkRule(id: String, ssid: String?, bssid: String?, actionMode: String, dnsHostname: String?) =
            Saved(id, ssid, bssid, actionMode, dnsHostname).let { saved += it; accept(it) }
        override fun deleteWifiNetworkRule(ruleId: String) { deleted += ruleId }
        override fun refreshWifiNetworks() { refreshed++ }
        override fun launchWifiSsidPermissionRequest() { permissionRequests++ }
        override fun setVpnDetectionEnabled(enabled: Boolean) = Unit
        override fun setVpnDetectionMode(mode: String) = Unit
        override fun setNetworkTypeDetectionEnabled(enabled: Boolean) = Unit
        override fun setNetworkTypeDetectionMode(mode: String) = Unit
        override fun setDnsStateOnWifi(state: String) = Unit
        override fun setDnsHostnameOnWifi(hostname: String?) = Unit
        override fun setWifiNetworkRulesEnabled(enabled: Boolean) = Unit
        override fun setDnsStateOnMobile(state: String) = Unit
        override fun setDnsHostnameOnMobile(hostname: String?) = Unit
    }
}
