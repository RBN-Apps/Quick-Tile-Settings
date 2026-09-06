package com.rbn.qtsettings

import android.content.Context
import com.google.gson.JsonParser
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.data.KnownWifiNetwork
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT

fun verifyMinifiedBackupRoundTrip(context: Context) {
    val preferencesManager = PreferencesManager.getInstance(context)
    val originalBackup = preferencesManager.exportSettingsBackupJson()
    try {
        preferencesManager.restoreSettingsBackupJson(LEGACY_1_3_BACKUP)
        val hostname = "minified-backup.example.com"
        val rules = listOf(
            WifiDnsRule(id = "ssid-off", ssid = " Campus ", actionMode = DNS_MODE_OFF),
            WifiDnsRule(
                id = "access-point", ssid = " Campus ", bssid = "AA:BB:CC:DD:EE:FF",
                actionMode = DNS_MODE_ON, dnsHostname = hostname
            ),
            WifiDnsRule(id = "ssid-auto", ssid = "Büro", actionMode = DNS_MODE_AUTO),
            WifiDnsRule(
                id = "bssid-default", bssid = "AA:BB:CC:DD:EE:01",
                actionMode = WIFI_DNS_ACTION_DEFAULT
            )
        )
        val history = listOf(
            KnownWifiNetwork(" Campus ", "AA:BB:CC:DD:EE:FF", 2000L),
            KnownWifiNetwork("Büro", null, 1000L)
        )
        preferencesManager.addCustomDnsHostname("Minified backup test", hostname)
        preferencesManager.setNetworkTypeDetectionEnabled(true)
        preferencesManager.setNetworkTypeDetectionMode(BACKGROUND_DETECTION)
        preferencesManager.setWifiNetworkRulesEnabled(true)
        rules.forEach { check(preferencesManager.addWifiNetworkRule(it)) }
        history.forEach {
            preferencesManager.recordKnownWifiNetwork(it.identity, it.lastSeenEpochMillis)
        }

        val backupJson = preferencesManager.exportSettingsBackupJson()
        val backupObject = JsonParser.parseString(backupJson).asJsonObject
        check(backupObject.get("schemaVersion").asInt == 1)
        val dns = backupObject.getAsJsonObject("dns")
        check(dns.get("wifiNetworkRulesEnabled").asBoolean)
        check(dns.get("networkTypeDetectionEnabled").asBoolean)
        check(dns.get("networkTypeDetectionMode").asString == BACKGROUND_DETECTION)
        val exportedRules = dns.getAsJsonArray("wifiNetworkRules")
        check(exportedRules.size() == rules.size)
        val accessPoint = exportedRules[1].asJsonObject
        check(accessPoint.keySet() == setOf("id", "ssid", "bssid", "actionMode", "dnsHostname"))
        check(accessPoint.get("ssid").asString == " Campus ")
        check(accessPoint.get("bssid").asString == "AA:BB:CC:DD:EE:FF")
        check(accessPoint.get("actionMode").asString == DNS_MODE_ON)
        check(accessPoint.get("dnsHostname").asString == hostname)
        val exportedHistory = dns.getAsJsonArray("knownWifiNetworks")
        check(exportedHistory.size() == history.size)
        check(exportedHistory[0].asJsonObject.keySet() == setOf("ssid", "bssid", "lastSeenEpochMillis"))
        check(exportedHistory[0].asJsonObject.get("lastSeenEpochMillis").asLong == 2000L)

        // Replace all backed-up data first so unchanged in-memory values cannot hide a broken restore.
        preferencesManager.restoreSettingsBackupJson(LEGACY_1_3_BACKUP)
        check(preferencesManager.getWifiNetworkRules().isEmpty())
        check(preferencesManager.getKnownWifiNetworks().isEmpty())
        check(!preferencesManager.areWifiNetworkRulesEnabled())
        check(preferencesManager.dnsHostnames.value.none { it.hostname == hostname })
        preferencesManager.restoreSettingsBackupJson(backupJson)

        val restoredEntry = preferencesManager.dnsHostnames.value.single { it.hostname == hostname }
        check(restoredEntry.name == "Minified backup test")
        check(preferencesManager.areWifiNetworkRulesEnabled())
        check(preferencesManager.isNetworkTypeDetectionEnabled())
        check(preferencesManager.getNetworkTypeDetectionMode() == BACKGROUND_DETECTION)
        check(preferencesManager.getWifiNetworkRules() == rules)
        check(preferencesManager.getKnownWifiNetworks() == history)

        // A pre-feature backup uses the same schema but has none of the three Wi-Fi fields.
        dns.remove("wifiNetworkRulesEnabled")
        dns.remove("wifiNetworkRules")
        dns.remove("knownWifiNetworks")
        preferencesManager.restoreSettingsBackupJson(backupObject.toString())
        check(!preferencesManager.areWifiNetworkRulesEnabled())
        check(preferencesManager.getWifiNetworkRules().isEmpty())
        check(preferencesManager.getKnownWifiNetworks().isEmpty())
    } finally {
        preferencesManager.restoreSettingsBackupJson(originalBackup)
    }
}

fun verifyLegacyMinifiedBackupRestore(context: Context) {
    val preferencesManager = PreferencesManager.getInstance(context)
    val originalBackup = preferencesManager.exportSettingsBackupJson()
    try {
        for ((json, id, hostname, sortMode) in listOf(
            listOf(LEGACY_1_3_BACKUP, "minified_legacy_13", "legacy13.example.com", "alphabetical"),
            listOf(LEGACY_1_4_BACKUP, "minified_legacy_14", "legacy14.example.com", "manual")
        )) {
            preferencesManager.setWifiNetworkRulesEnabled(true)
            check(preferencesManager.addWifiNetworkRule(WifiDnsRule(ssid = "Legacy reset")))
            preferencesManager.recordKnownWifiNetwork(WifiNetworkIdentity("Legacy reset"))
            preferencesManager.restoreSettingsBackupJson(json)
            check(preferencesManager.dnsHostnames.value.any { it.id == id && it.hostname == hostname })
            check(preferencesManager.dnsListSortMode.value == DnsListSortMode.fromPersistedValue(sortMode))
            check(!preferencesManager.areWifiNetworkRulesEnabled())
            check(preferencesManager.getWifiNetworkRules().isEmpty())
            check(preferencesManager.getKnownWifiNetworks().isEmpty())
        }
    } finally {
        preferencesManager.restoreSettingsBackupJson(originalBackup)
    }
}

private const val LEGACY_1_3_BACKUP = """
    {
      "a":1680000000000,
      "b":"2023-03-28T10:40:00Z",
      "c":{
        "a":true,"b":true,
        "c":[{"a":"minified_legacy_13","b":"Legacy 1.3","c":"legacy13.example.com","d":false,"e":true}],
        "d":false,"e":5,"f":false,"g":false,"h":"tile_only","i":false,"j":"tile_only",
        "k":"off","m":"opportunistic"
      },
      "d":{"a":true,"b":true,"c":false,"d":false,"e":false,"f":5,"g":false},
      "e":{"a":[],"b":[],"c":false},
      "f":{}
    }
"""

private const val LEGACY_1_4_BACKUP = """
    {
      "a":1690000000000,
      "b":"2023-07-22T04:26:40Z",
      "c":{
        "a":true,"b":true,
        "c":[{"a":"minified_legacy_14","b":"Legacy 1.4","c":"legacy14.example.com","d":false,"e":true}],
        "d":"manual","e":false,"f":5,"g":false,"h":false,"i":"tile_only","j":false,"k":"tile_only",
        "l":"off","n":"opportunistic"
      },
      "d":{"a":true,"b":true,"c":false,"d":false,"e":false,"f":5,"g":false},
      "e":{"a":[],"b":[],"c":false},
      "f":{}
    }
"""
