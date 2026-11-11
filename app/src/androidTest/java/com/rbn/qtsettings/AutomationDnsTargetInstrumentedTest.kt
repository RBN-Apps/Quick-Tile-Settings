package com.rbn.qtsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.automation.presentation.AutomationDnsTargetScreen
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AutomationDnsTargetInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchFindsProviderByNameAndHostname_withoutOfferingWifiRuleDefault() {
        var query by mutableStateOf("")
        var choice: Pair<String, String?>? = null
        compose.setContent { QuickTileSettingsTheme {
            AutomationDnsTargetScreen(DNS_MODE_AUTO, null, listOf(
                DnsHostnameEntry("cloudflare", "Cloudflare", "one.one.one.one"),
                DnsHostnameEntry("custom", "Work DNS", "dns.work.test")), query, { query = it },
                { mode, hostname -> choice = mode to hostname })
        } }
        compose.onNodeWithTag("automation_dns_target_page").assertIsDisplayed()
        compose.onNodeWithTag("automation_target_auto").assertIsSelected()
        compose.onNodeWithTag("automation_target_default").assertDoesNotExist()
        compose.onNodeWithTag("automation_dns_target_search").performTextInput("CLOUDFLARE")
        compose.onNodeWithTag("automation_target_cloudflare").assertIsDisplayed()
        compose.onNodeWithTag("automation_target_custom").assertDoesNotExist()
        assertNull(choice)
        compose.onNodeWithTag("automation_dns_target_search").performTextReplacement("DNS.WORK")
        compose.onNodeWithTag("automation_target_cloudflare").assertDoesNotExist()
        compose.onNodeWithTag("automation_target_custom").performClick()
        assertEquals(DNS_MODE_ON to "dns.work.test", choice)
    }

    @Test fun clearingUnmatchedSearchRestoresBuiltInTargets() {
        var query by mutableStateOf("unmatched")
        compose.setContent { QuickTileSettingsTheme {
            AutomationDnsTargetScreen(DNS_MODE_AUTO, null, emptyList(), query, { query = it }, { _, _ -> })
        } }
        compose.onNodeWithTag("automation_target_auto").assertDoesNotExist()
        compose.onNodeWithTag("automation_dns_target_search").performTextReplacement("")
        compose.onNodeWithTag("automation_target_auto").assertIsSelected()
        compose.onNodeWithTag("automation_target_off").assertIsDisplayed()
    }
}
