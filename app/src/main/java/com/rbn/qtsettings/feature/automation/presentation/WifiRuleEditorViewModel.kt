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
 * Editor draft scoped to one rule-editor navigation entry. Storage validation and commits stay
 * in AutomationViewModel; leaving the entry discards the draft.
 */
class WifiRuleEditorViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _draft = MutableStateFlow(saved.get<List<String>>(KEY_DRAFT)?.toDraft())
    val draft = _draft.asStateFlow()
    val targetStep = saved.getStateFlow("wifiRule.targetStep", false)
    val networkQuery = saved.getStateFlow("wifiRule.networkQuery", "")
    val targetQuery = saved.getStateFlow("wifiRule.targetQuery", "")
    fun searchNetworks(value: String) { saved["wifiRule.networkQuery"] = value }
    fun searchTargets(value: String) { saved["wifiRule.targetQuery"] = value }
    fun previous() { saved["wifiRule.targetStep"] = false }
    fun next() {
        val current = _draft.value ?: return
        val valid = WifiNetworkRuleUtils.normalizeRule(WifiDnsRule(
            current.id ?: "draft", current.ssid.takeIf { it.isNotBlank() },
            current.bssid.takeIf { it.isNotBlank() }, current.action, current.hostname)) != null
        update(current.copy(error = !valid))
        if (valid) saved["wifiRule.targetStep"] = true
    }
    val confirmDelete = saved.getStateFlow(KEY_CONFIRM_DELETE, false)

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
        if (!saved) {
            previous()
            update(draft.copy(error = true))
        }
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
