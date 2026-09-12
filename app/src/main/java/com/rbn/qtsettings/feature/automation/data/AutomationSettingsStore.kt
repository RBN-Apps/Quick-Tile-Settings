package com.rbn.qtsettings.feature.automation.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rbn.qtsettings.data.*
import com.rbn.qtsettings.feature.automation.domain.AutomationSettingsSource
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import com.rbn.qtsettings.utils.WifiNetworkRuleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single owner of the existing automation keys in qt_settings_prefs. */
class AutomationSettingsStore(private val sharedPreferences: SharedPreferences) : AutomationSettingsSource {
    private val gson = Gson()
    private val wifiDnsRuleListType = object : TypeToken<List<WifiDnsRule>>() {}.type
    private val knownWifiNetworkListType = object : TypeToken<List<KnownWifiNetwork>>() {}.type
    // VPN Detection Settings
    private val _vpnDetectionEnabled =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_VPN_DETECTION_ENABLED, false))
    override val vpnDetectionEnabled: StateFlow<Boolean> = _vpnDetectionEnabled.asStateFlow()

    private val _vpnDetectionMode =
        MutableStateFlow(
            sharedPreferences.getString(KEY_VPN_DETECTION_MODE, TILE_ONLY_DETECTION)
                ?: TILE_ONLY_DETECTION
        )
    override val vpnDetectionMode: StateFlow<String> = _vpnDetectionMode.asStateFlow()

    // Network Type Detection Settings
    private val _networkTypeDetectionEnabled =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, false))
    override val networkTypeDetectionEnabled: StateFlow<Boolean> = _networkTypeDetectionEnabled.asStateFlow()

    private val _networkTypeDetectionMode =
        MutableStateFlow(
            sharedPreferences.getString(KEY_NETWORK_TYPE_DETECTION_MODE, TILE_ONLY_DETECTION)
                ?: TILE_ONLY_DETECTION
        )
    override val networkTypeDetectionMode: StateFlow<String> = _networkTypeDetectionMode.asStateFlow()

    private val _dnsStateOnWifi =
        MutableStateFlow(
            sharedPreferences.getString(KEY_DNS_STATE_ON_WIFI, DNS_MODE_OFF)
                ?: DNS_MODE_OFF
        )
    override val dnsStateOnWifi: StateFlow<String> = _dnsStateOnWifi.asStateFlow()

    private val _dnsHostnameOnWifi =
        MutableStateFlow(sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_WIFI, null))
    override val dnsHostnameOnWifi: StateFlow<String?> = _dnsHostnameOnWifi.asStateFlow()

    private val _wifiNetworkRulesEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_WIFI_NETWORK_RULES_ENABLED, false)
    )
    override val wifiNetworkRulesEnabled: StateFlow<Boolean> = _wifiNetworkRulesEnabled.asStateFlow()

    private val _wifiNetworkRules = MutableStateFlow(loadWifiNetworkRules())
    override val wifiNetworkRules: StateFlow<List<WifiDnsRule>> = _wifiNetworkRules.asStateFlow()

    private val _knownWifiNetworks = MutableStateFlow(loadKnownWifiNetworks())
    override val knownWifiNetworks: StateFlow<List<KnownWifiNetwork>> = _knownWifiNetworks.asStateFlow()

    private val _dnsStateOnMobile =
        MutableStateFlow(
            sharedPreferences.getString(KEY_DNS_STATE_ON_MOBILE, DNS_MODE_AUTO)
                ?: DNS_MODE_AUTO
        )
    override val dnsStateOnMobile: StateFlow<String> = _dnsStateOnMobile.asStateFlow()

    private val _dnsHostnameOnMobile =
        MutableStateFlow(sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_MOBILE, null))
    override val dnsHostnameOnMobile: StateFlow<String?> = _dnsHostnameOnMobile.asStateFlow()

    override fun setVpnDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_VPN_DETECTION_ENABLED, enabled) }
        _vpnDetectionEnabled.value = enabled
    }

    override fun setVpnDetectionMode(mode: String) {
        sharedPreferences.edit { putString(KEY_VPN_DETECTION_MODE, mode) }
        _vpnDetectionMode.value = mode
    }

    override fun setNetworkTypeDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, enabled) }
        _networkTypeDetectionEnabled.value = enabled
    }

    override fun setNetworkTypeDetectionMode(mode: String) {
        sharedPreferences.edit { putString(KEY_NETWORK_TYPE_DETECTION_MODE, mode) }
        _networkTypeDetectionMode.value = mode
    }

    override fun setDnsStateOnWifi(state: String) {
        sharedPreferences.edit { putString(KEY_DNS_STATE_ON_WIFI, state) }
        _dnsStateOnWifi.value = state
    }

    override fun setDnsHostnameOnWifi(hostname: String?) {
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAME_ON_WIFI, hostname) }
        _dnsHostnameOnWifi.value = hostname
    }

    override fun setWifiNetworkRulesEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_WIFI_NETWORK_RULES_ENABLED, enabled) }
        _wifiNetworkRulesEnabled.value = enabled
    }

    override fun addWifiNetworkRule(rule: WifiDnsRule): Boolean {
        val normalizedRule = WifiNetworkRuleUtils.normalizeRule(rule) ?: return false
        if (_wifiNetworkRules.value.any { WifiNetworkRuleUtils.hasSameMatcher(it, normalizedRule) }) {
            return false
        }
        saveWifiNetworkRules(_wifiNetworkRules.value + normalizedRule)
        return true
    }

    override fun updateWifiNetworkRule(rule: WifiDnsRule): Boolean {
        val normalizedRule = WifiNetworkRuleUtils.normalizeRule(rule) ?: return false
        val existingRules = _wifiNetworkRules.value
        if (existingRules.none { it.id == normalizedRule.id } ||
            existingRules.any {
                it.id != normalizedRule.id &&
                        WifiNetworkRuleUtils.hasSameMatcher(it, normalizedRule)
            }
        ) {
            return false
        }
        saveWifiNetworkRules(
            existingRules.map { if (it.id == normalizedRule.id) normalizedRule else it }
        )
        return true
    }

    override fun deleteWifiNetworkRule(ruleId: String) {
        saveWifiNetworkRules(_wifiNetworkRules.value.filterNot { it.id == ruleId })
    }

    override fun recordKnownWifiNetwork(
        identity: WifiNetworkIdentity,
        seenAtEpochMillis: Long
    ) {
        val normalized = WifiNetworkRuleUtils.normalizeIdentity(identity) ?: return
        val updatedEntry = KnownWifiNetwork(
            ssid = normalized.ssid,
            bssid = normalized.bssid,
            lastSeenEpochMillis = seenAtEpochMillis
        )
        val updated = buildList {
            add(updatedEntry)
            addAll(
                _knownWifiNetworks.value.filterNot {
                    it.ssid == updatedEntry.ssid && it.bssid == updatedEntry.bssid
                }
            )
        }.sortedByDescending(KnownWifiNetwork::lastSeenEpochMillis)
            .take(MAX_KNOWN_WIFI_NETWORKS)
        saveKnownWifiNetworks(updated)
    }

    private fun saveWifiNetworkRules(rules: List<WifiDnsRule>) {
        val normalizedRules = rules.mapNotNull(WifiNetworkRuleUtils::normalizeRule)
            .distinctBy { "${it.ssid.orEmpty()}|${it.bssid.orEmpty()}" }
            .distinctBy(WifiDnsRule::id)
            .take(MAX_WIFI_NETWORK_RULES)
        sharedPreferences.edit {
            putString(KEY_WIFI_NETWORK_RULES, gson.toJson(normalizedRules))
        }
        _wifiNetworkRules.value = normalizedRules
    }

    private fun saveKnownWifiNetworks(networks: List<KnownWifiNetwork>) {
        val normalizedNetworks = networks.mapNotNull { network ->
            WifiNetworkRuleUtils.normalizeIdentity(network.identity)?.let { identity ->
                network.copy(ssid = identity.ssid, bssid = identity.bssid)
            }
        }.distinctBy { "${it.ssid.orEmpty()}|${it.bssid.orEmpty()}" }
            .sortedByDescending(KnownWifiNetwork::lastSeenEpochMillis)
            .take(MAX_KNOWN_WIFI_NETWORKS)
        sharedPreferences.edit {
            putString(KEY_KNOWN_WIFI_NETWORKS, gson.toJson(normalizedNetworks))
        }
        _knownWifiNetworks.value = normalizedNetworks
    }

    override fun setDnsStateOnMobile(state: String) {
        sharedPreferences.edit { putString(KEY_DNS_STATE_ON_MOBILE, state) }
        _dnsStateOnMobile.value = state
    }

    override fun setDnsHostnameOnMobile(hostname: String?) {
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAME_ON_MOBILE, hostname) }
        _dnsHostnameOnMobile.value = hostname
    }

    private fun loadWifiNetworkRules(): List<WifiDnsRule> {
        return try {
            gson.fromJson<List<WifiDnsRule>>(
                sharedPreferences.getString(KEY_WIFI_NETWORK_RULES, null),
                wifiDnsRuleListType
            ).orEmpty().mapNotNull(WifiNetworkRuleUtils::normalizeRule)
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error loading Wi-Fi DNS rules", e)
            emptyList()
        }
    }

    private fun loadKnownWifiNetworks(): List<KnownWifiNetwork> {
        return try {
            gson.fromJson<List<KnownWifiNetwork>>(
                sharedPreferences.getString(KEY_KNOWN_WIFI_NETWORKS, null),
                knownWifiNetworkListType
            ).orEmpty().mapNotNull { network ->
                WifiNetworkRuleUtils.normalizeIdentity(network.identity)?.let { identity ->
                    network.copy(ssid = identity.ssid, bssid = identity.bssid)
                }
            }.sortedByDescending(KnownWifiNetwork::lastSeenEpochMillis)
                .take(MAX_KNOWN_WIFI_NETWORKS)
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Error loading known Wi-Fi networks", e)
            emptyList()
        }
    }

    override fun isVpnDetectionEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_VPN_DETECTION_ENABLED, false)

    override fun getVpnDetectionMode(): String =
        sharedPreferences.getString(KEY_VPN_DETECTION_MODE, TILE_ONLY_DETECTION)
            ?: TILE_ONLY_DETECTION

    override fun isNetworkTypeDetectionEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, false)

    override fun getNetworkTypeDetectionMode(): String =
        sharedPreferences.getString(KEY_NETWORK_TYPE_DETECTION_MODE, TILE_ONLY_DETECTION)
            ?: TILE_ONLY_DETECTION

    override fun getDnsStateOnWifi(): String =
        sharedPreferences.getString(KEY_DNS_STATE_ON_WIFI, DNS_MODE_OFF)
            ?: DNS_MODE_OFF

    override fun getDnsHostnameOnWifi(): String? =
        sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_WIFI, null)

    override fun areWifiNetworkRulesEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_WIFI_NETWORK_RULES_ENABLED, false)

    override fun getWifiNetworkRules(): List<WifiDnsRule> = _wifiNetworkRules.value

    override fun getKnownWifiNetworks(): List<KnownWifiNetwork> = _knownWifiNetworks.value

    override fun getDnsStateOnMobile(): String =
        sharedPreferences.getString(KEY_DNS_STATE_ON_MOBILE, DNS_MODE_AUTO)
            ?: DNS_MODE_AUTO

    override fun getDnsHostnameOnMobile(): String? =
        sharedPreferences.getString(KEY_DNS_HOSTNAME_ON_MOBILE, null)

    fun writeTo(editor: SharedPreferences.Editor, backup: DnsSettingsBackup) = with(editor) {
        putBoolean(KEY_VPN_DETECTION_ENABLED, backup.vpnDetectionEnabled)
        putString(KEY_VPN_DETECTION_MODE, backup.vpnDetectionMode)
        putBoolean(KEY_NETWORK_TYPE_DETECTION_ENABLED, backup.networkTypeDetectionEnabled)
        putString(KEY_NETWORK_TYPE_DETECTION_MODE, backup.networkTypeDetectionMode)
        putString(KEY_DNS_STATE_ON_WIFI, backup.dnsStateOnWifi)
        putString(KEY_DNS_HOSTNAME_ON_WIFI, backup.dnsHostnameOnWifi)
        putBoolean(KEY_WIFI_NETWORK_RULES_ENABLED, backup.wifiNetworkRulesEnabled)
        putString(KEY_WIFI_NETWORK_RULES, gson.toJson(backup.wifiNetworkRules))
        putString(KEY_KNOWN_WIFI_NETWORKS, gson.toJson(backup.knownWifiNetworks))
        putString(KEY_DNS_STATE_ON_MOBILE, backup.dnsStateOnMobile)
        putString(KEY_DNS_HOSTNAME_ON_MOBILE, backup.dnsHostnameOnMobile)
    }

    fun refresh() {
        _vpnDetectionEnabled.value = isVpnDetectionEnabled()
        _vpnDetectionMode.value = getVpnDetectionMode()
        _networkTypeDetectionEnabled.value = isNetworkTypeDetectionEnabled()
        _networkTypeDetectionMode.value = getNetworkTypeDetectionMode()
        _dnsStateOnWifi.value = getDnsStateOnWifi()
        _dnsHostnameOnWifi.value = getDnsHostnameOnWifi()
        _wifiNetworkRulesEnabled.value = areWifiNetworkRulesEnabled()
        _wifiNetworkRules.value = loadWifiNetworkRules()
        _knownWifiNetworks.value = loadKnownWifiNetworks()
        _dnsStateOnMobile.value = getDnsStateOnMobile()
        _dnsHostnameOnMobile.value = getDnsHostnameOnMobile()
    }

    companion object {
        private const val KEY_VPN_DETECTION_ENABLED = "vpn_detection_enabled"
        private const val KEY_VPN_DETECTION_MODE = "vpn_detection_mode"

        private const val KEY_NETWORK_TYPE_DETECTION_ENABLED = "network_type_detection_enabled"
        private const val KEY_NETWORK_TYPE_DETECTION_MODE = "network_type_detection_mode"
        private const val KEY_DNS_STATE_ON_WIFI = "dns_state_on_wifi"
        private const val KEY_DNS_HOSTNAME_ON_WIFI = "dns_hostname_on_wifi"
        private const val KEY_WIFI_NETWORK_RULES_ENABLED = "wifi_network_rules_enabled_v1"
        private const val KEY_WIFI_NETWORK_RULES = "wifi_network_dns_rules_v1"
        private const val KEY_KNOWN_WIFI_NETWORKS = "known_wifi_networks_v1"
        private const val KEY_DNS_STATE_ON_MOBILE = "dns_state_on_mobile"
        private const val KEY_DNS_HOSTNAME_ON_MOBILE = "dns_hostname_on_mobile"
        private const val MAX_WIFI_NETWORK_RULES = 250
        private const val MAX_KNOWN_WIFI_NETWORKS = 100
    }
}
