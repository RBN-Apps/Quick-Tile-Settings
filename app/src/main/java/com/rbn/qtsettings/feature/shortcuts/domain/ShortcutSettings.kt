package com.rbn.qtsettings.feature.shortcuts.domain

import kotlinx.coroutines.flow.StateFlow

data class ShortcutConfiguration(
    val maxCount: Int = 0,
    val enabledIds: Set<String> = emptySet(),
    val favoriteIds: Set<String> = emptySet(),
    val allowPinnedWhenDisabled: Boolean = false
)

interface ShortcutSettings {
    val state: StateFlow<ShortcutConfiguration>
    fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean): Boolean
    fun setShortcutFavorite(shortcutId: String, favorite: Boolean): Boolean
    fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean)
    fun refreshShortcutConfiguration()
}
