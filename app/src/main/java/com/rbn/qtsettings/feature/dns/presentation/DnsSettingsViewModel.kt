package com.rbn.qtsettings.feature.dns.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.dns.domain.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DnsSettingsViewModel(
    private val store: DnsSettingsSource,
    private val actions: DnsActions,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(DnsSettingsState(settings = store.read()))
    val state = mutableState.asStateFlow()
    private val mutableEditor = MutableStateFlow(readEditor())
    val editor = mutableEditor.asStateFlow()
    val pendingDeletion = savedState.getStateFlow<String?>("dns.delete", null)
    private val messages = Channel<DnsMessage>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    init {
        refresh()
        viewModelScope.launch { store.state.collect { settings -> mutableState.update { it.copy(settings = settings) } } }
    }
    fun refresh() {
        mutableState.update { it.copy(settings = store.read(), active = actions.read(), hasPermission = actions.hasPermission()) }
    }
    fun onAction(action: DnsSettingsAction) {
        when (action) {
            is DnsSettingsAction.ToggleOff -> store.setDnsToggleOff(action.value)
            is DnsSettingsAction.ToggleAuto -> store.setDnsToggleAuto(action.value)
            is DnsSettingsAction.Selected -> store.setSelected(action.id, action.value)
            is DnsSettingsAction.ManualOrder -> store.setManualOrder(action.value)
            is DnsSettingsAction.EnableAutoRevert -> store.setDnsEnableAutoRevert(action.value)
            is DnsSettingsAction.RevertDelay -> store.setDnsAutoRevertDelaySeconds(action.value)
            is DnsSettingsAction.RequireUnlock -> store.setDnsRequireUnlock(action.value)
            is DnsSettingsAction.Activate -> {
                val result = actions.set(action.target)
                val message = when (result) {
                    DnsActionResult.Success -> when (action.target) {
                        DnsTarget.Off -> DnsMessage(R.string.shortcut_toast_dns_off)
                        DnsTarget.Automatic -> DnsMessage(R.string.shortcut_toast_dns_auto)
                        is DnsTarget.Hostname -> DnsMessage(R.string.shortcut_toast_dns_hostname, action.name ?: action.target.value)
                    }
                    DnsActionResult.PermissionMissing -> DnsMessage(R.string.toast_permission_not_granted_adb)
                    DnsActionResult.InvalidHostname -> DnsMessage(R.string.error_hostname_value_invalid)
                    else -> DnsMessage(R.string.toast_error_saving_settings)
                }
                messages.trySend(message)
            }
            is DnsSettingsAction.Edit -> {
                val provider = store.read().providers.find { it.id == action.id && !it.predefined }
                if (action.id != null && provider == null) return
                setEditor(DnsEditorState(true, provider?.id, provider?.name.orEmpty(), provider?.target?.value.orEmpty()))
            }
            is DnsSettingsAction.Delete -> savedState["dns.delete"] = action.id
        }
        refresh()
    }
    fun reorder(ids: List<String>): Boolean = store.reorder(ids).also { refresh() }
    fun editName(value: String) = setEditor(editor.value.copy(name = value, nameError = null))
    fun editHostname(value: String) = setEditor(editor.value.copy(hostname = value, hostnameError = null))
    fun dismissEditor() = setEditor(DnsEditorState())
    fun saveEditor() {
        val draft = editor.value
        val name = draft.name.trim()
        val hostname = draft.hostname.trim()
        val validation = validateDnsEditor(name, hostname)
        if (!validation.valid) {
            setEditor(draft.copy(name = name, hostname = hostname, nameError = validation.nameError, hostnameError = validation.hostnameError))
            return
        }
        if (draft.id == null) store.add(name, hostname) else store.edit(draft.id, name, hostname)
        dismissEditor()
        refresh()
    }
    fun confirmDeletion() {
        pendingDeletion.value?.let(store::delete)
        savedState["dns.delete"] = null
        refresh()
    }
    private fun setEditor(value: DnsEditorState) {
        savedState["dns.edit.visible"] = value.visible
        savedState["dns.edit.id"] = value.id
        savedState["dns.edit.name"] = value.name
        savedState["dns.edit.hostname"] = value.hostname
        savedState["dns.edit.nameError"] = value.nameError?.name
        savedState["dns.edit.hostnameError"] = value.hostnameError?.name
        mutableEditor.value = value
    }
    private fun readEditor() = DnsEditorState(
        savedState["dns.edit.visible"] ?: false,
        savedState["dns.edit.id"],
        savedState["dns.edit.name"] ?: "",
        savedState["dns.edit.hostname"] ?: "",
        savedState.get<String>("dns.edit.nameError")?.let(DnsEditorError::valueOf),
        savedState.get<String>("dns.edit.hostnameError")?.let(DnsEditorError::valueOf)
    )
}
