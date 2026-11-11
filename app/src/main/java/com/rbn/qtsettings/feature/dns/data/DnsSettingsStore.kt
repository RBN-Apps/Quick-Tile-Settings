package com.rbn.qtsettings.feature.dns.data

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.rbn.qtsettings.data.*
import com.rbn.qtsettings.feature.dns.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owns the unchanged DNS preference keys and legacy hostname JSON. */
class DnsSettingsStore(private val sharedPreferences: SharedPreferences) : DnsSettingsSource {
    private val gson = Gson()
    var onProvidersChanged: () -> Unit = {}
    // DNS Settings
    private val _dnsToggleOff =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true))
    val dnsToggleOff: StateFlow<Boolean> = _dnsToggleOff.asStateFlow()

    private val _dnsToggleAuto =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true))
    val dnsToggleAuto: StateFlow<Boolean> = _dnsToggleAuto.asStateFlow()

    private val hostnameEntryListType = object : TypeToken<List<DnsHostnameEntry>>() {}.type
    private val _dnsListSortMode = MutableStateFlow(
        DnsListSortMode.fromPersistedValue(
            sharedPreferences.getString(KEY_DNS_LIST_SORT_MODE, null)
        )
    )
    val dnsListSortMode: StateFlow<DnsListSortMode> = _dnsListSortMode.asStateFlow()

    private var manualDnsHostnames: List<DnsHostnameEntry> = emptyList()
    private val _dnsHostnames = MutableStateFlow<List<DnsHostnameEntry>>(emptyList())
    val dnsHostnames: StateFlow<List<DnsHostnameEntry>> = _dnsHostnames.asStateFlow()

    private val _dnsEnableAutoRevert =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_ENABLE_AUTO_REVERT, false))
    val dnsEnableAutoRevert: StateFlow<Boolean> = _dnsEnableAutoRevert.asStateFlow()

    private val _dnsAutoRevertDelaySeconds =
        MutableStateFlow(sharedPreferences.getInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, 5))
    val dnsAutoRevertDelaySeconds: StateFlow<Int> = _dnsAutoRevertDelaySeconds.asStateFlow()

    private val _dnsRequireUnlock =
        MutableStateFlow(sharedPreferences.getBoolean(KEY_DNS_REQUIRE_UNLOCK, false))
    val dnsRequireUnlock: StateFlow<Boolean> = _dnsRequireUnlock.asStateFlow()

    private val mutableState = MutableStateFlow(DnsSettings())
    override val state = mutableState.asStateFlow()
    init { loadDnsHostnames(); publishState() }

    override fun read(): DnsSettings = DnsSettings(
        toggleOff = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true),
        toggleAuto = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true),
        providers = getAllDnsHostnamesBlocking().map { it.toProvider() },
        manualOrder = getDnsListSortModeBlocking() == DnsListSortMode.MANUAL,
        enableAutoRevert = sharedPreferences.getBoolean(KEY_DNS_ENABLE_AUTO_REVERT, false),
        autoRevertDelaySeconds = sharedPreferences.getInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, 5),
        requireUnlock = sharedPreferences.getBoolean(KEY_DNS_REQUIRE_UNLOCK, false)
    )
    private fun publishState() { mutableState.value = read() }
    fun getDnsListSortModeBlocking() = DnsListSortMode.fromPersistedValue(sharedPreferences.getString(KEY_DNS_LIST_SORT_MODE, null))
    fun manualEntries(): List<DnsHostnameEntry> = readManualDnsHostnamesBlocking()
    override fun setSelected(id: String, selected: Boolean) {
        dnsHostnames.value.find { it.id == id }?.let { updateDnsHostnameEntry(it.copy(isSelectedForCycle = selected)) }
    }
    override fun edit(id: String, name: String, hostname: String) {
        dnsHostnames.value.find { it.id == id && !it.isPredefined }?.let { updateDnsHostnameEntry(it.copy(name = name, hostname = hostname)) }
    }
    override fun setManualOrder(manual: Boolean) = setDnsListSortMode(if (manual) DnsListSortMode.MANUAL else DnsListSortMode.ALPHABETICAL)
    override fun reorder(ids: List<String>) = reorderDnsHostnames(ids)
    override fun add(name: String, hostname: String) = addCustomDnsHostname(name, hostname)
    override fun delete(id: String) = deleteCustomDnsHostname(id)

    /** Joins the validated cross-feature backup transaction without changing the schema. */
    fun writeTo(editor: SharedPreferences.Editor, backup: DnsSettingsBackup) {
        editor.putBoolean(KEY_DNS_TOGGLE_OFF, backup.toggleOff)
        editor.putBoolean(KEY_DNS_TOGGLE_AUTO, backup.toggleAuto)
        editor.putString(KEY_DNS_HOSTNAMES, gson.toJson(backup.hostnames))
        editor.putString(KEY_DNS_LIST_SORT_MODE, backup.sortMode)
        editor.putBoolean(KEY_DNS_ENABLE_AUTO_REVERT, backup.enableAutoRevert)
        editor.putInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, backup.autoRevertDelaySeconds)
        editor.putBoolean(KEY_DNS_REQUIRE_UNLOCK, backup.requireUnlock)
    }
    fun refresh() {
        _dnsToggleOff.value = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_OFF, true)
        _dnsToggleAuto.value = sharedPreferences.getBoolean(KEY_DNS_TOGGLE_AUTO, true)
        _dnsEnableAutoRevert.value = sharedPreferences.getBoolean(KEY_DNS_ENABLE_AUTO_REVERT, false)
        _dnsAutoRevertDelaySeconds.value = sharedPreferences.getInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, 5)
        _dnsRequireUnlock.value = sharedPreferences.getBoolean(KEY_DNS_REQUIRE_UNLOCK, false)
        _dnsListSortMode.value = getDnsListSortModeBlocking()
        manualDnsHostnames = readManualDnsHostnamesBlocking()
        publishEffectiveDnsHostnames()
        publishState()
    }
    override fun setDnsToggleOff(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_OFF, enabled) }
        _dnsToggleOff.value = enabled
        publishState()
    }

    override fun setDnsToggleAuto(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_TOGGLE_AUTO, enabled) }
        _dnsToggleAuto.value = enabled
        publishState()
    }

    fun setDnsListSortMode(mode: DnsListSortMode) {
        if (_dnsListSortMode.value == mode) return

        sharedPreferences.edit { putString(KEY_DNS_LIST_SORT_MODE, mode.persistedValue) }
        _dnsListSortMode.value = mode
        publishEffectiveDnsHostnames()
        publishState()
        onProvidersChanged()
    }

    override fun setDnsEnableAutoRevert(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_ENABLE_AUTO_REVERT, enabled) }
        _dnsEnableAutoRevert.value = enabled
        publishState()
    }

    override fun setDnsAutoRevertDelaySeconds(delay: Int) {
        sharedPreferences.edit { putInt(KEY_DNS_AUTO_REVERT_DELAY_SECONDS, delay) }
        _dnsAutoRevertDelaySeconds.value = delay
        publishState()
    }

    override fun setDnsRequireUnlock(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DNS_REQUIRE_UNLOCK, enabled) }
        _dnsRequireUnlock.value = enabled
        publishState()
    }

    private fun publishEffectiveDnsHostnames() {
        _dnsHostnames.value = DnsHostnamePolicy.sort(
            hostnames = manualDnsHostnames,
            mode = _dnsListSortMode.value
        )
    }

    private fun loadDnsHostnames() {
        val storedHostnames = parseStoredDnsHostnames(
            json = sharedPreferences.getString(KEY_DNS_HOSTNAMES, null),
            errorMessage = "Error parsing stored DNS hostnames"
        )

        manualDnsHostnames = DnsHostnamePolicy.reconcile(
            storedHostnames = storedHostnames,
            normalizeCustomEntries = false
        )
        publishEffectiveDnsHostnames()
        saveDnsHostnamesInternal()
        publishState()
        saveDnsListSortModeInternal()
    }

    private fun parseStoredDnsHostnames(
        json: String?,
        errorMessage: String
    ): List<DnsHostnameEntry>? {
        if (json == null) return null

        return try {
            val parsed = JsonParser.parseString(json)
            if (!parsed.isJsonArray) return null

            gson.fromJson(
                LegacyMinifiedDnsHostnameJson.normalizeArray(parsed.asJsonArray),
                hostnameEntryListType
            )
        } catch (e: Exception) {
            Log.e("DnsSettingsStore", errorMessage, e)
            null
        }
    }

    private fun saveDnsHostnamesInternal() {
        val json = gson.toJson(manualDnsHostnames)
        sharedPreferences.edit { putString(KEY_DNS_HOSTNAMES, json) }
    }

    private fun saveDnsListSortModeInternal() {
        sharedPreferences.edit {
            putString(KEY_DNS_LIST_SORT_MODE, _dnsListSortMode.value.persistedValue)
        }
    }

    fun updateDnsHostnameEntry(updatedEntry: DnsHostnameEntry) {
        val currentList = manualDnsHostnames.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedEntry.id }
        if (index != -1) {
            currentList[index] = updatedEntry
            manualDnsHostnames = currentList
            publishEffectiveDnsHostnames()
            saveDnsHostnamesInternal()
            publishState()
            onProvidersChanged()
        }
    }

    fun addCustomDnsHostname(name: String, hostnameValue: String) {
        val newEntry = DnsHostnameEntry(
            name = name,
            hostname = hostnameValue,
            isPredefined = false,
            isSelectedForCycle = true
        )
        val newList = manualDnsHostnames.toMutableList()
        newList.add(newEntry)
        manualDnsHostnames = newList
        publishEffectiveDnsHostnames()
        saveDnsHostnamesInternal()
        publishState()
        onProvidersChanged()
    }

    fun deleteCustomDnsHostname(id: String) {
        val currentList = manualDnsHostnames.toMutableList()
        val removed = currentList.removeAll { it.id == id && !it.isPredefined }
        if (!removed) return

        manualDnsHostnames = currentList
        publishEffectiveDnsHostnames()
        saveDnsHostnamesInternal()
        publishState()
        onProvidersChanged()
    }

    fun reorderDnsHostnames(orderedIds: List<String>): Boolean {
        if (_dnsListSortMode.value != DnsListSortMode.MANUAL) return false
        if (orderedIds.size != manualDnsHostnames.size) return false
        if (orderedIds.toSet().size != orderedIds.size) return false

        val entriesById = manualDnsHostnames.associateBy { it.id }
        if (entriesById.size != manualDnsHostnames.size || orderedIds.toSet() != entriesById.keys) {
            return false
        }

        val reordered = orderedIds.map { entryId -> entriesById.getValue(entryId) }
        if (reordered == manualDnsHostnames) return true

        manualDnsHostnames = reordered
        publishEffectiveDnsHostnames()
        saveDnsHostnamesInternal()
        publishState()
        onProvidersChanged()
        return true
    }

    fun getDnsHostnamesSelectedForCycle(): List<DnsHostnameEntry> {
        return getAllDnsHostnamesBlocking().filter { it.isSelectedForCycle }
    }

    fun getAllDnsHostnamesBlocking(): List<DnsHostnameEntry> {
        val manualHostnames = readManualDnsHostnamesBlocking()
        return DnsHostnamePolicy.sort(
            hostnames = manualHostnames,
            mode = getDnsListSortModeBlocking()
        )
    }

    private fun readManualDnsHostnamesBlocking(): List<DnsHostnameEntry> {
        val storedHostnames = parseStoredDnsHostnames(
            json = sharedPreferences.getString(KEY_DNS_HOSTNAMES, null),
            errorMessage = "Error parsing stored DNS hostnames for blocking read"
        )
        return DnsHostnamePolicy.reconcile(
            storedHostnames = storedHostnames ?: manualDnsHostnames,
            normalizeCustomEntries = false
        )
    }
    companion object {
        private const val KEY_DNS_TOGGLE_OFF = "dns_toggle_off"
        private const val KEY_DNS_TOGGLE_AUTO = "dns_toggle_auto"
        private const val KEY_DNS_ENABLE_AUTO_REVERT = "dns_enable_auto_revert"
        private const val KEY_DNS_AUTO_REVERT_DELAY_SECONDS = "dns_auto_revert_delay_seconds"
        private const val KEY_DNS_HOSTNAMES = "dns_hostnames_list_v2"
        private const val KEY_DNS_LIST_SORT_MODE = "dns_list_sort_mode_v1"
        private const val KEY_DNS_REQUIRE_UNLOCK = "dns_require_unlock"
    }
}
