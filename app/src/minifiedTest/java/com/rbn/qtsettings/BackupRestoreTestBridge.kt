package com.rbn.qtsettings

import android.content.Context
import com.google.gson.JsonParser
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.data.PreferencesManager

fun verifyMinifiedBackupRoundTrip(context: Context) {
    val preferencesManager = PreferencesManager.getInstance(context)
    val hostname = "minified-backup-${System.nanoTime()}.example.com"

    preferencesManager.addCustomDnsHostname("Minified backup test", hostname)

    val backupJson = preferencesManager.exportSettingsBackupJson()
    preferencesManager.restoreSettingsBackupJson(backupJson)

    val backupObject = JsonParser.parseString(backupJson).asJsonObject
    check(backupObject.get("schemaVersion") != null)
    check(backupObject.getAsJsonObject("dns").get("hostnames") != null)

    val restoredEntry = preferencesManager.dnsHostnames.value
        .firstOrNull { it.hostname == hostname }
    checkNotNull(restoredEntry)
    check(restoredEntry.name == "Minified backup test")
}

fun verifyLegacyMinifiedBackupRestore(context: Context) {
    val preferencesManager = PreferencesManager.getInstance(context)

    preferencesManager.restoreSettingsBackupJson(LEGACY_1_3_BACKUP)
    check(preferencesManager.dnsHostnames.value.any {
        it.id == "minified_legacy_13" && it.hostname == "legacy13.example.com"
    })
    check(preferencesManager.dnsListSortMode.value == DnsListSortMode.ALPHABETICAL)

    preferencesManager.restoreSettingsBackupJson(LEGACY_1_4_BACKUP)
    check(preferencesManager.dnsHostnames.value.any {
        it.id == "minified_legacy_14" && it.hostname == "legacy14.example.com"
    })
    check(preferencesManager.dnsListSortMode.value == DnsListSortMode.MANUAL)
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
