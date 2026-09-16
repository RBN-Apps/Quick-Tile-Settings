package com.rbn.qtsettings.feature.usb.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.usb.domain.UsbActionResult
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.domain.UsbAutoRevert
import com.rbn.qtsettings.feature.usb.domain.UsbSettings
import com.rbn.qtsettings.feature.usb.domain.UsbSettingsSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsbSettingsViewModel(
    private val store: UsbSettingsSource,
    private val actions: UsbActions,
    private val autoRevert: UsbAutoRevert
) : ViewModel() {
    private val mutableState = MutableStateFlow(UsbSettingsState(settings = store.read()))
    val state = mutableState.asStateFlow()
    private val messages = Channel<Int>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    init {
        refresh()
        viewModelScope.launch { store.state.collect { settings -> mutableState.update { it.copy(settings = settings) } } }
    }
    fun refresh() {
        mutableState.update { it.copy(system = actions.read(), hasPermission = actions.hasPermission(), settings = store.read()) }
    }
    fun onAction(action: UsbSettingsAction) {
        when (action) {
            is UsbSettingsAction.ToggleEnable -> store.setToggleEnable(action.value)
            is UsbSettingsAction.ToggleDisable -> store.setToggleDisable(action.value)
            is UsbSettingsAction.AlsoHideDevOptions -> store.setAlsoHideDevOptions(action.value)
            is UsbSettingsAction.AlsoDisableWirelessDebugging -> store.setAlsoDisableWirelessDebugging(action.value)
            is UsbSettingsAction.EnableAutoRevert -> store.setEnableAutoRevert(action.value)
            is UsbSettingsAction.AutoRevertDelaySeconds -> store.setAutoRevertDelaySeconds(action.value)
            is UsbSettingsAction.RequireUnlock -> store.setRequireUnlock(action.value)
            is UsbSettingsAction.SetActive -> {
                val settings = store.read()
                val result = actions.set(action.enabled, settings.alsoHideDevOptions, settings.alsoDisableWirelessDebugging)
                messages.trySend(when (result) {
                    UsbActionResult.Success -> if (action.enabled) R.string.shortcut_toast_usb_on else R.string.shortcut_toast_usb_off
                    UsbActionResult.PermissionMissing -> R.string.toast_permission_not_granted_adb
                    UsbActionResult.DeveloperOptionsDisabled -> R.string.toast_developer_options_disabled
                    else -> R.string.toast_error_saving_settings
                })
            }
            UsbSettingsAction.Cycle -> {
                val settings = store.read()
                val cycle = actions.cycle(settings)
                messages.trySend(if (cycle.result == UsbActionResult.Success) {
                    armRevert(settings, cycle)
                    if (actions.read().usb) R.string.shortcut_toast_usb_on else R.string.shortcut_toast_usb_off
                } else errorFor(cycle.result))
            }
        }
        refresh()
    }

    /** The tile's revert is process-wide, so an in-app cycle gets exactly the same one. */
    private fun armRevert(settings: UsbSettings, cycle: UsbActions.CycleResult) {
        val previous = cycle.previous ?: return
        if (settings.enableAutoRevert && settings.autoRevertDelaySeconds > 0) {
            autoRevert.schedule(previous, cycle.generation, settings.autoRevertDelaySeconds, store::read) { _, _ -> refresh() }
        } else {
            autoRevert.cancel()
        }
    }

    private fun errorFor(result: UsbActionResult) = when (result) {
        UsbActionResult.PermissionMissing -> R.string.toast_permission_not_granted_adb
        UsbActionResult.DeveloperOptionsDisabled -> R.string.toast_developer_options_disabled
        UsbActionResult.NoCycleStates -> R.string.toast_no_states_enabled_usb
        else -> R.string.toast_error_saving_settings
    }
}
