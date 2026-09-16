package com.rbn.qtsettings.feature.automation.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.WifiNetworkRuleUtils
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Editor draft shared by the two rule-editor pages. Both steps are separate navigation entries, so
 * the draft outlives each one; opening the editor begins a new one via [startSession]. Storage
 * validation and commits stay in AutomationViewModel.
 */
class WifiRuleEditorViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _draft = MutableStateFlow(saved.get<List<String>>(KEY_DRAFT)?.toDraft())
    val draft = _draft.asStateFlow()
    val networkQuery = saved.getStateFlow(KEY_NETWORK_QUERY, "")
    val targetQuery = saved.getStateFlow(KEY_TARGET_QUERY, "")
    fun searchNetworks(value: String) { saved[KEY_NETWORK_QUERY] = value }
    fun searchTargets(value: String) { saved[KEY_TARGET_QUERY] = value }

    /** Reports whether the identity may advance to the target step and marks the field otherwise. */
    fun validateNetwork(): Boolean {
        val current = _draft.value ?: return false
        val valid = WifiNetworkRuleUtils.normalizeRule(WifiDnsRule(
            current.id ?: "draft", current.ssid.takeIf { it.isNotBlank() },
            current.bssid.takeIf { it.isNotBlank() }, current.action, current.hostname)) != null
        update(current.copy(error = !valid))
        return valid
    }
    val confirmDelete = saved.getStateFlow(KEY_CONFIRM_DELETE, false)

    /** Begins a new editor session, dropping whatever draft the previous one left behind. */
    fun startSession(rule: WifiDnsRule?) {
        reset()
        start(rule)
    }

    private fun reset() {
        saved.remove<Any>(KEY_DRAFT)
        saved[KEY_NETWORK_QUERY] = ""
        saved[KEY_TARGET_QUERY] = ""
        saved[KEY_CONFIRM_DELETE] = false
        _draft.value = null
    }

    /** Seeds the draft once; a restored draft keeps the user's edits over the stored rule. */
    fun start(rule: WifiDnsRule?) {
        if (_draft.value != null) return
        update(rule?.let {
            WifiRuleDraft(it.id, it.ssid.orEmpty(), it.bssid.orEmpty(), it.actionMode, it.dnsHostname)
        } ?: WifiRuleDraft())
    }

    fun ssid(value: String) = edit { copy(ssid = value, error = false) }
    fun bssid(value: String) = edit { copy(bssid = value, error = false) }
    fun target(mode: String, hostname: String?) = edit { copy(action = mode, hostname = hostname, error = false) }

    /** Copies the name only; matching a single access point stays an explicit choice. */
    fun useNetwork(identity: WifiNetworkIdentity) = edit {
        copy(ssid = identity.ssid.orEmpty(),
            bssid = if (identity.ssid == null) identity.bssid.orEmpty() else "",
            detectedBssid = identity.bssid, error = false)
    }

    fun useDetectedBssid() = edit { copy(bssid = detectedBssid ?: bssid, error = false) }

    /** Returns true when storage accepted the rule; otherwise the draft stays open with an error. */
    fun save(save: (id: String?, ssid: String?, bssid: String?, action: String, hostname: String?) -> Boolean): Boolean {
        val draft = _draft.value ?: return false
        val saved = save(draft.id, draft.ssid.takeIf { it.isNotBlank() },
            draft.bssid.takeIf { it.isNotBlank() }, draft.action, draft.hostname)
        if (!saved) update(draft.copy(error = true))
        return saved
    }

    fun requestDelete() { saved[KEY_CONFIRM_DELETE] = true }
    fun cancelDelete() { saved[KEY_CONFIRM_DELETE] = false }

    private fun edit(change: WifiRuleDraft.() -> WifiRuleDraft) { _draft.value?.let { update(it.change()) } }

    private fun update(value: WifiRuleDraft) {
        saved[KEY_DRAFT] = listOf(value.id.orEmpty(), value.ssid, value.bssid, value.action,
            value.hostname.orEmpty(), value.detectedBssid.orEmpty(), value.error.toString())
        _draft.value = value
    }

    private fun List<String>.toDraft() = WifiRuleDraft(
        get(0).ifEmpty { null }, get(1), get(2), get(3), get(4).ifEmpty { null },
        get(5).ifEmpty { null }, get(6).toBoolean())

    private companion object {
        const val KEY_DRAFT = "wifiRule.draft"
        const val KEY_CONFIRM_DELETE = "wifiRule.confirmDelete"
        const val KEY_NETWORK_QUERY = "wifiRule.networkQuery"
        const val KEY_TARGET_QUERY = "wifiRule.targetQuery"
    }
}

data class WifiRuleDraft(
    val id: String? = null,
    val ssid: String = "",
    val bssid: String = "",
    val action: String = DNS_MODE_OFF,
    val hostname: String? = null,
    val detectedBssid: String? = null,
    val error: Boolean = false
) {
    val identity: String get() = listOf(ssid, bssid).filter { it.isNotBlank() }.joinToString(" / ")
}
