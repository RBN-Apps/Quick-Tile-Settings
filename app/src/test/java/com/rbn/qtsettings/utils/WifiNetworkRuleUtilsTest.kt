package com.rbn.qtsettings.utils

import android.net.wifi.WifiManager
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WifiNetworkRuleUtilsTest {

    private val defaultTarget = PrivateDnsTarget(DNS_MODE_ON, "dns.example.com")
    private val disabledTarget = PrivateDnsTarget(DNS_MODE_OFF)

    @Test
    fun resolveDnsTarget_whenRulesAreDisabledAndSsidIsListed_returnsDefaultTarget() {
        assertEquals(
            defaultTarget,
            resolve(rulesEnabled = false, "Campus")
        )
    }

    @Test
    fun resolveDnsTarget_whenRulesAreDisabledAndSsidIsNotListed_returnsDefaultTarget() {
        assertEquals(
            defaultTarget,
            resolve(rulesEnabled = false, "Home")
        )
    }

    @Test
    fun resolveDnsTarget_whenEnabledRuleMatches_returnsRuleTarget() {
        assertEquals(
            disabledTarget,
            resolve(rulesEnabled = true, "Campus")
        )
    }

    @Test
    fun resolveDnsTarget_whenNoEnabledRuleMatches_returnsDefaultTarget() {
        assertEquals(
            defaultTarget,
            resolve(rulesEnabled = true, "Home")
        )
    }

    @Test
    fun resolveDnsTarget_whenRulesAreEnabledButEmpty_returnsDefaultTarget() {
        assertEquals(
            defaultTarget,
            resolve(rulesEnabled = true, "unknown", rules = emptyList())
        )
    }

    @Test
    fun resolveDnsTarget_whenIdentityIsUnknownAndRulesAreEnabled_returnsNoTarget() {
        val unknownSsids = listOf(null, WifiManager.UNKNOWN_SSID, "", "\"\"")

        unknownSsids.forEach { unknownSsid ->
            assertNull(resolve(rulesEnabled = true, unknownSsid))
        }
    }

    @Test
    fun normalizeDetectedSsid_whenFrameworkValueIsQuoted_removesOnlyOuterQuotes() {
        assertEquals(
            "Campus \"Guest\"",
            WifiNetworkRuleUtils.normalizeDetectedSsid("\"Campus \"Guest\"\"")
        )
        assertEquals(
            "Campus",
            WifiNetworkRuleUtils.normalizeDetectedSsid("Campus")
        )
        assertNull(WifiNetworkRuleUtils.normalizeDetectedSsid("\"\""))
    }

    @Test
    fun resolveDnsTarget_whenDetectedSsidIsQuoted_matchesStoredUnquotedSsid() {
        assertEquals(
            disabledTarget,
            resolve(rulesEnabled = true, "\"Campus\"")
        )
    }

    @Test
    fun resolveDnsTarget_matchesSsidsCaseSensitively() {
        assertEquals(
            disabledTarget,
            resolve(rulesEnabled = true, "Campus")
        )
        assertEquals(
            defaultTarget,
            resolve(rulesEnabled = true, "campus")
        )
        assertEquals(
            "CaMpUs",
            WifiNetworkRuleUtils.normalizeDetectedSsid("\"CaMpUs\"")
        )
    }

    @Test
    fun isValidSsid_enforcesMaximumLengthInUtf8Bytes() {
        assertFalse(WifiNetworkRuleUtils.isValidSsid(""))

        assertTrue(WifiNetworkRuleUtils.isValidSsid("a".repeat(32)))
        assertFalse(WifiNetworkRuleUtils.isValidSsid("a".repeat(33)))

        assertTrue(WifiNetworkRuleUtils.isValidSsid("ä".repeat(16)))
        assertFalse(WifiNetworkRuleUtils.isValidSsid("ä".repeat(17)))

        assertTrue(WifiNetworkRuleUtils.isValidSsid("🙂".repeat(8)))
        assertFalse(WifiNetworkRuleUtils.isValidSsid("🙂".repeat(9)))
    }

    @Test
    fun resolveDnsTarget_explicitHostnameRuleSelectsPerNetworkDns() {
        val target = WifiNetworkRuleUtils.resolveDnsTarget(
            rulesEnabled = true,
            rules = listOf(
                WifiDnsRule(
                    ssid = "Campus",
                    actionMode = DNS_MODE_ON,
                    dnsHostname = "campus.example.com"
                )
            ),
            currentNetwork = WifiNetworkIdentity(ssid = "Campus"),
            defaultTarget = defaultTarget
        )

        assertEquals(PrivateDnsTarget(DNS_MODE_ON, "campus.example.com"), target)
    }

    @Test
    fun resolveDnsTarget_bssidSpecificRuleTakesPrecedenceOverSsidRule() {
        val rules = listOf(
            WifiDnsRule(ssid = "Campus", actionMode = DNS_MODE_OFF),
            WifiDnsRule(
                ssid = "Campus",
                bssid = "AA:BB:CC:DD:EE:FF",
                actionMode = DNS_MODE_AUTO
            )
        )

        assertEquals(
            PrivateDnsTarget(DNS_MODE_AUTO),
            WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = true,
                rules = rules,
                currentNetwork = WifiNetworkIdentity("Campus", "aa:bb:cc:dd:ee:ff"),
                defaultTarget = defaultTarget
            )
        )
        assertEquals(
            disabledTarget,
            WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = true,
                rules = rules,
                currentNetwork = WifiNetworkIdentity("Campus", "11:22:33:44:55:66"),
                defaultTarget = defaultTarget
            )
        )
    }

    @Test
    fun resolveDnsTarget_combinedMatcherTakesPrecedenceOverBssidOnlyRule() {
        assertEquals(
            PrivateDnsTarget(DNS_MODE_AUTO),
            WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = true,
                rules = listOf(
                    WifiDnsRule(
                        bssid = "AA:BB:CC:DD:EE:FF",
                        actionMode = DNS_MODE_OFF
                    ),
                    WifiDnsRule(
                        ssid = "Campus",
                        bssid = "AA:BB:CC:DD:EE:FF",
                        actionMode = DNS_MODE_AUTO
                    )
                ),
                currentNetwork = WifiNetworkIdentity("Campus", "AA:BB:CC:DD:EE:FF"),
                defaultTarget = defaultTarget
            )
        )
    }

    @Test
    fun resolveDnsTarget_defaultActionUsesConfiguredFallback() {
        assertEquals(
            defaultTarget,
            WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = true,
                rules = listOf(
                    WifiDnsRule(ssid = "Home", actionMode = WIFI_DNS_ACTION_DEFAULT)
                ),
                currentNetwork = WifiNetworkIdentity(ssid = "Home"),
                defaultTarget = defaultTarget
            )
        )
    }

    @Test
    fun resolveDnsTarget_unknownIdentityLeavesDnsUnchangedWhenRulesAreEnabled() {
        assertNull(
            WifiNetworkRuleUtils.resolveDnsTarget(
                rulesEnabled = true,
                rules = listOf(WifiDnsRule(ssid = "Campus", actionMode = DNS_MODE_OFF)),
                currentNetwork = WifiNetworkIdentity(
                    ssid = WifiNetworkRuleUtils.normalizeDetectedSsid(WifiManager.UNKNOWN_SSID),
                    bssid = "02:00:00:00:00:00"
                ),
                defaultTarget = defaultTarget
            )
        )
    }

    @Test
    fun literalQuotedSsid_staysDistinctThroughNormalizationAndMatching() {
        val literalSsid = "\"Campus\""
        val rule = WifiNetworkRuleUtils.normalizeRule(
            WifiDnsRule(ssid = literalSsid, actionMode = DNS_MODE_OFF)
        )!!
        assertEquals(literalSsid, WifiNetworkRuleUtils.normalizeRule(rule)?.ssid)
        assertEquals(defaultTarget, WifiNetworkRuleUtils.resolveDnsTarget(
            true, listOf(rule), WifiNetworkIdentity("Campus"), defaultTarget
        ))
        val detected = WifiNetworkIdentity(
            WifiNetworkRuleUtils.normalizeDetectedSsid("\"$literalSsid\"")
        )
        assertEquals(disabledTarget, WifiNetworkRuleUtils.resolveDnsTarget(
            true, listOf(rule), WifiNetworkRuleUtils.normalizeIdentity(detected), defaultTarget
        ))
    }

    @Test
    fun normalizeRule_rejectsInvalidExplicitSsidInsteadOfBroadeningMatcher() {
        listOf("a".repeat(33), "ä".repeat(17), "   ").forEach { invalidSsid ->
            assertNull(WifiNetworkRuleUtils.normalizeRule(
                WifiDnsRule(ssid = invalidSsid, bssid = "AA:BB:CC:DD:EE:FF")
            ))
        }
    }

    @Test
    fun normalizeRule_rejectsInvalidExplicitBssidInsteadOfBroadeningMatcher() {
        assertNull(
            WifiNetworkRuleUtils.normalizeRule(
                WifiDnsRule(
                    ssid = "Campus",
                    bssid = "not-a-mac",
                    actionMode = DNS_MODE_OFF
                )
            )
        )
        assertEquals(
            "AA:BB:CC:DD:EE:FF",
            WifiNetworkRuleUtils.normalizeRule(
                WifiDnsRule(
                    ssid = "Campus",
                    bssid = "aa:bb:cc:dd:ee:ff",
                    actionMode = DNS_MODE_OFF
                )
            )?.bssid
        )
    }

    private fun resolve(
        rulesEnabled: Boolean,
        currentSsid: String?,
        rules: List<WifiDnsRule> = listOf(
            WifiDnsRule(
                ssid = "Campus",
                actionMode = DNS_MODE_OFF
            )
        )
    ): PrivateDnsTarget? {
        return WifiNetworkRuleUtils.resolveDnsTarget(
            rulesEnabled = rulesEnabled,
            rules = rules,
            currentNetwork = WifiNetworkIdentity(
                ssid = WifiNetworkRuleUtils.normalizeDetectedSsid(currentSsid)
            ),
            defaultTarget = defaultTarget
        )
    }
}
