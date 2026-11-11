package com.rbn.qtsettings.feature.dns.presentation

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.DnsHostnamePolicy
import com.rbn.qtsettings.feature.dns.domain.*

data class DnsSettingsState(
    val settings: DnsSettings = DnsSettings(),
    val active: DnsTarget? = null,
    val hasPermission: Boolean = false
)

data class DnsEditorState(
    val visible: Boolean = false,
    val id: String? = null,
    val name: String = "",
    val hostname: String = "",
    val nameError: DnsEditorError? = null,
    val hostnameError: DnsEditorError? = null
)

sealed interface DnsSettingsAction {
    data class ToggleOff(val value: Boolean) : DnsSettingsAction
    data class ToggleAuto(val value: Boolean) : DnsSettingsAction
    data class Selected(val id: String, val value: Boolean) : DnsSettingsAction
    data class ManualOrder(val value: Boolean) : DnsSettingsAction
    data class EnableAutoRevert(val value: Boolean) : DnsSettingsAction
    data class RevertDelay(val value: Int) : DnsSettingsAction
    data class RequireUnlock(val value: Boolean) : DnsSettingsAction
    data class Activate(val target: DnsTarget, val name: String? = null) : DnsSettingsAction
    data object Cycle : DnsSettingsAction
    data class Edit(val id: String?) : DnsSettingsAction
    data class Delete(val id: String?) : DnsSettingsAction
}

data class DnsMessage(val resource: Int, val argument: String? = null)

/** Existing row model is retained until #116; resource metadata stays out of domain providers. */
internal fun DnsProvider.toRowEntry() = DnsHostnameEntry(
    id, name, target.value, predefined, selectedForCycle,
    if (predefined) DnsHostnamePolicy.defaultHostnames().find { it.id == id }?.descriptionResId else null
)
