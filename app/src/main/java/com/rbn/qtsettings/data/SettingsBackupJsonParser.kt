package com.rbn.qtsettings.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal class SettingsBackupJsonParser(
    private val gson: Gson
) {
    fun parse(json: String): SettingsBackup {
        val root = JsonParser.parseString(json)
        require(root.isJsonObject) { "Invalid backup file" }

        val rootObject = root.asJsonObject
        val normalizedRoot = when {
            rootObject.has(STABLE_SCHEMA_VERSION_KEY) -> rootObject
            isLegacyMinifiedBackup(rootObject, LegacyVersion.V1_3) ->
                migrateLegacyMinifiedBackup(rootObject, LegacyVersion.V1_3)
            isLegacyMinifiedBackup(rootObject, LegacyVersion.V1_4) ->
                migrateLegacyMinifiedBackup(rootObject, LegacyVersion.V1_4)
            else -> rootObject
        }

        return gson.fromJson(normalizedRoot, SettingsBackup::class.java)
            ?: throw IllegalArgumentException("Invalid backup file")
    }

    private fun isLegacyMinifiedBackup(
        root: JsonObject,
        version: LegacyVersion
    ): Boolean {
        if (root.keySet() != LEGACY_ROOT_KEYS) return false
        if (!root.isNumber("a") || !root.isString("b")) return false

        val dns = root.objectOrNull("c") ?: return false
        if (root.objectOrNull("d") == null || root.objectOrNull("e") == null) return false
        if (root.objectOrNull("f") == null || dns.get("c")?.isJsonArray != true) return false

        val discriminator = dns.get("d") ?: return false
        return discriminator.isJsonPrimitive && when (version) {
            LegacyVersion.V1_3 -> discriminator.asJsonPrimitive.isBoolean
            LegacyVersion.V1_4 -> discriminator.asJsonPrimitive.isString
        }
    }

    private fun migrateLegacyMinifiedBackup(
        root: JsonObject,
        version: LegacyVersion
    ): JsonObject = JsonObject().apply {
        addProperty(STABLE_SCHEMA_VERSION_KEY, SettingsBackup.CURRENT_SCHEMA_VERSION)
        copy(root, "a", "exportedAtEpochMillis")
        copy(root, "b", "exportedAtIso8601")
        add("dns", migrateDns(root.getAsJsonObject("c"), version))
        add("usb", migrateUsb(root.getAsJsonObject("d")))
        add("shortcuts", migrateShortcuts(root.getAsJsonObject("e")))
        copy(root, "f", "features")
    }

    private fun migrateDns(source: JsonObject, version: LegacyVersion): JsonObject =
        JsonObject().apply {
            copy(source, "a", "toggleOff")
            copy(source, "b", "toggleAuto")
            add("hostnames", migrateHostnames(source.getAsJsonArray("c")))

            when (version) {
                LegacyVersion.V1_3 -> {
                    addProperty("sortMode", DnsListSortMode.ALPHABETICAL.persistedValue)
                    copy(source, "d", "enableAutoRevert")
                    copy(source, "e", "autoRevertDelaySeconds")
                    copy(source, "f", "requireUnlock")
                    copy(source, "g", "vpnDetectionEnabled")
                    copy(source, "h", "vpnDetectionMode")
                    copy(source, "i", "networkTypeDetectionEnabled")
                    copy(source, "j", "networkTypeDetectionMode")
                    copy(source, "k", "dnsStateOnWifi")
                    copy(source, "l", "dnsHostnameOnWifi")
                    copy(source, "m", "dnsStateOnMobile")
                    copy(source, "n", "dnsHostnameOnMobile")
                }
                LegacyVersion.V1_4 -> {
                    copy(source, "d", "sortMode")
                    copy(source, "e", "enableAutoRevert")
                    copy(source, "f", "autoRevertDelaySeconds")
                    copy(source, "g", "requireUnlock")
                    copy(source, "h", "vpnDetectionEnabled")
                    copy(source, "i", "vpnDetectionMode")
                    copy(source, "j", "networkTypeDetectionEnabled")
                    copy(source, "k", "networkTypeDetectionMode")
                    copy(source, "l", "dnsStateOnWifi")
                    copy(source, "m", "dnsHostnameOnWifi")
                    copy(source, "n", "dnsStateOnMobile")
                    copy(source, "o", "dnsHostnameOnMobile")
                }
            }
        }

    private fun migrateHostnames(source: JsonArray): JsonArray {
        source.forEach { element ->
            require(element.isJsonObject) { "Invalid legacy DNS hostname entry" }
        }
        return LegacyMinifiedDnsHostnameJson.normalizeArray(source)
    }

    private fun migrateUsb(source: JsonObject): JsonObject = JsonObject().apply {
        copy(source, "a", "toggleEnable")
        copy(source, "b", "toggleDisable")
        copy(source, "c", "alsoHideDevOptions")
        copy(source, "d", "alsoDisableWirelessDebugging")
        copy(source, "e", "enableAutoRevert")
        copy(source, "f", "autoRevertDelaySeconds")
        copy(source, "g", "requireUnlock")
    }

    private fun migrateShortcuts(source: JsonObject): JsonObject = JsonObject().apply {
        copy(source, "a", "enabledShortcutIds")
        copy(source, "b", "favoriteShortcutIds")
        copy(source, "c", "allowPinnedShortcutsWhenDisabled")
    }

    private fun JsonObject.copy(source: JsonObject, sourceKey: String, targetKey: String) {
        source.get(sourceKey)?.let { add(targetKey, it.deepCopy()) }
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.isNumber(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.isNumber == true

    private fun JsonObject.isString(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.isString == true

    private enum class LegacyVersion {
        V1_3,
        V1_4
    }

    private companion object {
        const val STABLE_SCHEMA_VERSION_KEY = "schemaVersion"
        val LEGACY_ROOT_KEYS = setOf("a", "b", "c", "d", "e", "f")
    }
}
