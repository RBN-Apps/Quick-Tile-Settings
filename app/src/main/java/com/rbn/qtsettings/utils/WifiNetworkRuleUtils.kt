package com.rbn.qtsettings.utils

import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

object WifiNetworkRuleUtils {

    /** Converts the quoted framework representation into the exact, case-sensitive SSID. */
    fun normalizeDetectedSsid(rawSsid: String?): String? {
        val ssid = rawSsid?.takeUnless { it == UNKNOWN_SSID } ?: return null
        return if (ssid.length >= 2 && ssid.first() == '"' && ssid.last() == '"') {
            ssid.substring(1, ssid.length - 1)
        } else {
            ssid
        }.takeIf { it.isNotEmpty() }
    }

    fun normalizeIdentity(identity: WifiNetworkIdentity): WifiNetworkIdentity? {
        // Identities already contain the literal SSID, not WifiInfo's quoted representation.
        val ssid = identity.ssid?.takeIf { it.isNotEmpty() }
        val bssid = normalizeBssid(identity.bssid)
        return if (ssid == null && bssid == null) null else WifiNetworkIdentity(ssid, bssid)
    }

    fun normalizeRule(rule: WifiDnsRule): WifiDnsRule? {
        val ssid = rule.ssid?.takeIf { it.isNotEmpty() }
        if (ssid != null && !isValidSsid(ssid)) return null
        val bssid = normalizeBssid(rule.bssid)
        if (!rule.bssid.isNullOrBlank() && bssid == null) return null
        if (ssid == null && bssid == null) return null

        val actionMode = rule.actionMode.takeIf(VALID_ACTIONS::contains)
            ?: WIFI_DNS_ACTION_DEFAULT
        val hostname = rule.dnsHostname
            ?.trim()
            ?.takeIf { actionMode == DNS_MODE_ON && it.isNotEmpty() }

        return rule.copy(
            id = rule.id.takeIf { it.isNotBlank() } ?: return null,
            ssid = ssid,
            bssid = bssid,
            actionMode = if (actionMode == DNS_MODE_ON && hostname == null) {
                WIFI_DNS_ACTION_DEFAULT
            } else {
                actionMode
            },
            dnsHostname = hostname
        )
    }

    fun normalizeBssid(rawBssid: String?): String? {
        val bssid = rawBssid?.trim()?.uppercase() ?: return null
        if (bssid == REDACTED_BSSID || bssid == BROADCAST_BSSID) return null
        return bssid.takeIf { BSSID_REGEX.matches(it) }
    }

    fun hasSameMatcher(first: WifiDnsRule, second: WifiDnsRule): Boolean {
        val normalizedFirst = normalizeRule(first) ?: return false
        val normalizedSecond = normalizeRule(second) ?: return false
        return normalizedFirst.ssid == normalizedSecond.ssid &&
                normalizedFirst.bssid == normalizedSecond.bssid
    }

    fun isValidSsid(ssid: String): Boolean {
        return ssid.isNotBlank() && ssid.toByteArray(Charsets.UTF_8).size <= MAX_SSID_BYTES
    }

    private const val MAX_SSID_BYTES = 32
    private const val UNKNOWN_SSID = "<unknown ssid>"
    private const val REDACTED_BSSID = "02:00:00:00:00:00"
    private const val BROADCAST_BSSID = "FF:FF:FF:FF:FF:FF"
    private val BSSID_REGEX = Regex("^(?:[0-9A-F]{2}:){5}[0-9A-F]{2}$")
    private val VALID_ACTIONS = setOf(
        WIFI_DNS_ACTION_DEFAULT,
        DNS_MODE_OFF,
        DNS_MODE_AUTO,
        DNS_MODE_ON
    )

}
