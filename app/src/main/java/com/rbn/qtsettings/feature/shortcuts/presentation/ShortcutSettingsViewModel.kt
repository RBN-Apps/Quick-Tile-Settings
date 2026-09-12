package com.rbn.qtsettings.feature.shortcuts.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutConfiguration
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal enum class ShortcutDialog { None, Limit, FavoriteLimit, PinnedInfo, PinnedWarning }
internal data class ShortcutSettingsState(
    val configuration: ShortcutConfiguration,
    val dnsItems: List<ShortcutUiItem>,
    val usbItems: List<ShortcutUiItem> = buildUsbShortcutItems(),
    val dialog: ShortcutDialog = ShortcutDialog.None,
    val infoExpanded: Boolean = false
)
internal sealed interface ShortcutSettingsAction {
    data class Toggle(val id: String, val enabled: Boolean) : ShortcutSettingsAction
    data class Favorite(val id: String, val enabled: Boolean) : ShortcutSettingsAction
    data class SetPinnedFallback(val enabled: Boolean) : ShortcutSettingsAction
    data class ShowDialog(val dialog: ShortcutDialog) : ShortcutSettingsAction
    data object DismissDialog : ShortcutSettingsAction
    data object ToggleInfo : ShortcutSettingsAction
}

class ShortcutSettingsViewModel(
    private val settings: ShortcutSettings,
    providers: StateFlow<List<DnsHostnameEntry>>,
    private val saved: SavedStateHandle
) : ViewModel() {
    private val dialog = saved.getStateFlow("shortcut.dialog", ShortcutDialog.None.name)
    private val expanded = saved.getStateFlow("shortcut.infoExpanded", false)
    internal val state = combine(settings.state, providers, dialog, expanded) { config, entries, openDialog, info ->
        ShortcutSettingsState(config, buildPrivateDnsShortcutItems(entries), dialog = ShortcutDialog.valueOf(openDialog), infoExpanded = info)
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        ShortcutSettingsState(settings.state.value, buildPrivateDnsShortcutItems(providers.value),
            dialog = ShortcutDialog.valueOf(dialog.value), infoExpanded = expanded.value))

    internal fun onAction(action: ShortcutSettingsAction) {
        when (action) {
            is ShortcutSettingsAction.Toggle -> if (!settings.setShortcutExposureEnabled(action.id, action.enabled))
                saved["shortcut.dialog"] = ShortcutDialog.Limit.name
            is ShortcutSettingsAction.Favorite -> if (!settings.setShortcutFavorite(action.id, action.enabled))
                saved["shortcut.dialog"] = ShortcutDialog.FavoriteLimit.name
            is ShortcutSettingsAction.SetPinnedFallback -> settings.setAllowPinnedShortcutsWhenDisabled(action.enabled)
            is ShortcutSettingsAction.ShowDialog -> saved["shortcut.dialog"] = action.dialog.name
            ShortcutSettingsAction.DismissDialog -> saved["shortcut.dialog"] = ShortcutDialog.None.name
            ShortcutSettingsAction.ToggleInfo -> saved["shortcut.infoExpanded"] = !expanded.value
        }
    }
}
