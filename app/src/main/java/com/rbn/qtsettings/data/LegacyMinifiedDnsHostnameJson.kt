package com.rbn.qtsettings.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Releases up to 1.4.0 were minified without stable Gson field names, so every
 * [DnsHostnameEntry] they persisted — both into the backup file and into
 * `dns_hostnames_list_v2` — carries R8's obfuscated field names instead of the real ones.
 * Reading such an entry with the current stable names yields an all-null object, which
 * [DnsHostnamePolicy.reconcile] silently drops.
 *
 * This maps the obfuscated names back. The order matches the declaration order in
 * [DnsHostnameEntry], which is what R8 assigns names from.
 */
internal object LegacyMinifiedDnsHostnameJson {

    private val FIELD_NAMES = listOf(
        "a" to "id",
        "b" to "name",
        "c" to "hostname",
        "d" to "isPredefined",
        "e" to "isSelectedForCycle",
        "f" to "descriptionResId"
    )

    private val LEGACY_KEYS = FIELD_NAMES.map { it.first }.toSet()

    fun normalizeArray(source: JsonArray): JsonArray = JsonArray().apply {
        source.forEach { element -> add(normalizeElement(element)) }
    }

    private fun normalizeElement(element: JsonElement): JsonElement {
        if (!element.isJsonObject) return element

        val entry = element.asJsonObject
        if (!isLegacyMinifiedEntry(entry)) return entry

        return JsonObject().apply {
            FIELD_NAMES.forEach { (legacyKey, stableKey) ->
                entry.get(legacyKey)?.let { add(stableKey, it.deepCopy()) }
            }
        }
    }

    private fun isLegacyMinifiedEntry(entry: JsonObject): Boolean {
        if (entry.keySet().isEmpty() || !LEGACY_KEYS.containsAll(entry.keySet())) return false

        // name and hostname are non-null in DnsHostnameEntry, so both are always serialized.
        if (!entry.isString("b") || !entry.isString("c")) return false
        if (entry.has("a") && !entry.isString("a")) return false
        if (entry.has("d") && !entry.isBoolean("d")) return false
        if (entry.has("e") && !entry.isBoolean("e")) return false
        if (entry.has("f") && !entry.isNumber("f")) return false
        return true
    }

    private fun JsonObject.isString(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.isString == true

    private fun JsonObject.isBoolean(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.isBoolean == true

    private fun JsonObject.isNumber(key: String): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.isNumber == true
}
