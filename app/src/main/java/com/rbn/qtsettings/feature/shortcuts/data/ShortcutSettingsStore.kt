package com.rbn.qtsettings.feature.shortcuts.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.ShortcutSettingsBackup
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutConfiguration
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutPublisher
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owns shortcut preferences; the existing preference file, keys and defaults are retained. */
class ShortcutSettingsStore(
    private val sharedPreferences: SharedPreferences,
    private val providers: StateFlow<List<DnsHostnameEntry>>,
    private val publisher: ShortcutPublisher
) : ShortcutSettings {
    private val _state = MutableStateFlow(ShortcutConfiguration())
    override val state = _state.asStateFlow()
    private fun publishState() {
        _state.value = ShortcutConfiguration(_shortcutMaxCount.value, _enabledShortcutIds.value,
            _favoriteShortcutIds.value, _allowPinnedShortcutsWhenDisabled.value)
    }
    // App Shortcut Settings
    private val _shortcutMaxCount = MutableStateFlow(publisher.maxCount())
    val shortcutMaxCount: StateFlow<Int> = _shortcutMaxCount.asStateFlow()

    private val _enabledShortcutIds = MutableStateFlow<Set<String>>(emptySet())
    val enabledShortcutIds: StateFlow<Set<String>> = _enabledShortcutIds.asStateFlow()
    private val _favoriteShortcutIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteShortcutIds: StateFlow<Set<String>> = _favoriteShortcutIds.asStateFlow()
    private val _allowPinnedShortcutsWhenDisabled = MutableStateFlow(false)
    val allowPinnedShortcutsWhenDisabled: StateFlow<Boolean> =
        _allowPinnedShortcutsWhenDisabled.asStateFlow()

    init {
        loadEnabledShortcutIds()
        loadFavoriteShortcutIds()
        loadAllowPinnedShortcutsWhenDisabled()
        refreshShortcutConfiguration()
    }

    fun reconcileProviders() {
        synchronizeEnabledShortcutsWithAvailableShortcuts()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    fun writeTo(editor: SharedPreferences.Editor, backup: ShortcutSettingsBackup) {
        editor.putBoolean(KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED, backup.allowPinnedShortcutsWhenDisabled)
    }

    fun restore(backup: ShortcutSettingsBackup) {
        _enabledShortcutIds.value = ShortcutCatalog.migrateLegacyShortcutIds(backup.enabledShortcutIds)
        _favoriteShortcutIds.value = ShortcutCatalog.migrateLegacyShortcutIds(backup.favoriteShortcutIds)
        _allowPinnedShortcutsWhenDisabled.value = backup.allowPinnedShortcutsWhenDisabled
        reconcileProviders()
    }
    override fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED, enabled) }
        _allowPinnedShortcutsWhenDisabled.value = enabled
        publishState()
    }

    override fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean): Boolean {
        val availableShortcutIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        if (!availableShortcutIds.contains(shortcutId)) {
            return false
        }

        if (enabled) {
            if (_enabledShortcutIds.value.contains(shortcutId)) {
                return true
            }
            if (_enabledShortcutIds.value.size >= _shortcutMaxCount.value) {
                return false
            }
            _enabledShortcutIds.value = _enabledShortcutIds.value + shortcutId
        } else {
            _enabledShortcutIds.value = _enabledShortcutIds.value - shortcutId
            _favoriteShortcutIds.value = _favoriteShortcutIds.value - shortcutId
            saveFavoriteShortcutIdsInternal()
        }

        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
        return true
    }

    override fun setShortcutFavorite(shortcutId: String, favorite: Boolean): Boolean {
        val availableShortcutIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        if (!availableShortcutIds.contains(shortcutId)) {
            return false
        }

        if (favorite) {
            if (!_enabledShortcutIds.value.contains(shortcutId)) {
                return false
            }
            if (_favoriteShortcutIds.value.contains(shortcutId)) {
                return true
            }
            if (_favoriteShortcutIds.value.size >= ShortcutCatalog.MAX_FAVORITE_SHORTCUTS) {
                return false
            }
            _favoriteShortcutIds.value = _favoriteShortcutIds.value + shortcutId
        } else {
            if (!_favoriteShortcutIds.value.contains(shortcutId)) {
                return true
            }
            _favoriteShortcutIds.value = _favoriteShortcutIds.value - shortcutId
        }

        _favoriteShortcutIds.value = normalizeFavoriteIds(_favoriteShortcutIds.value)
        saveFavoriteShortcutIdsInternal()
        updateExposedShortcuts()
        return true
    }

    override fun refreshShortcutConfiguration() {
        _shortcutMaxCount.value = publisher.maxCount()
        synchronizeEnabledShortcutsWithAvailableShortcuts()
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
        synchronizeFavoriteShortcutsWithAvailableShortcuts()
        updateExposedShortcuts()
    }

    private fun loadEnabledShortcutIds() {
        val storedIds = sharedPreferences.getStringSet(KEY_ENABLED_SHORTCUT_IDS, null)?.toSet()
        val migratedStoredIds = storedIds?.let { ShortcutCatalog.migrateLegacyShortcutIds(it) }
        val availableIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)

        _enabledShortcutIds.value = if (migratedStoredIds == null) {
            emptySet()
        } else {
            migratedStoredIds.intersect(availableIds)
        }
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
    }

    private fun loadFavoriteShortcutIds() {
        val storedFavoriteIds =
            sharedPreferences.getStringSet(KEY_FAVORITE_SHORTCUT_IDS, null)?.toSet()
        val availableIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        _favoriteShortcutIds.value = normalizeFavoriteIds(
            storedFavoriteIds
                ?.intersect(availableIds)
                ?.intersect(_enabledShortcutIds.value)
                ?: emptySet()
        )
        saveFavoriteShortcutIdsInternal()
    }

    private fun loadAllowPinnedShortcutsWhenDisabled() {
        _allowPinnedShortcutsWhenDisabled.value = sharedPreferences.getBoolean(
            KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED,
            false
        )
    }

    private fun synchronizeEnabledShortcutsWithAvailableShortcuts() {
        val availableIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        _enabledShortcutIds.value = _enabledShortcutIds.value.intersect(availableIds)
        _enabledShortcutIds.value = limitShortcutIdsToMaxCount(_enabledShortcutIds.value)
        saveEnabledShortcutIdsInternal()
    }

    private fun synchronizeFavoriteShortcutsWithAvailableShortcuts() {
        val availableIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        _favoriteShortcutIds.value = normalizeFavoriteIds(
            _favoriteShortcutIds.value
                .intersect(availableIds)
                .intersect(_enabledShortcutIds.value)
        )
        saveFavoriteShortcutIdsInternal()
    }

    private fun limitShortcutIdsToMaxCount(shortcutIds: Set<String>): Set<String> {
        if (shortcutIds.size <= _shortcutMaxCount.value) return shortcutIds

        val preferredOrder = ShortcutCatalog.getOrderedShortcutIds(
            hostnames = providers.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
        return preferredOrder
            .filter { shortcutIds.contains(it) }
            .take(_shortcutMaxCount.value)
            .toSet()
    }

    private fun normalizeFavoriteIds(shortcutIds: Set<String>): Set<String> {
        val preferredOrder = ShortcutCatalog.getOrderedShortcutIds(providers.value)
        val enabledIds = _enabledShortcutIds.value
        return preferredOrder
            .filter { shortcutIds.contains(it) && enabledIds.contains(it) }
            .take(ShortcutCatalog.MAX_FAVORITE_SHORTCUTS)
            .toSet()
    }

    private fun updateExposedShortcuts() {
        publishState()
        publisher.publish(
            dnsHostnames = providers.value,
            enabledShortcutIds = _enabledShortcutIds.value,
            favoriteShortcutIds = _favoriteShortcutIds.value
        )
    }

    fun canExecuteShortcutId(shortcutId: String): Boolean {
        val availableIds = ShortcutCatalog.getAvailableShortcutIds(providers.value)
        if (!availableIds.contains(shortcutId)) {
            return false
        }
        return _enabledShortcutIds.value.contains(shortcutId) || _allowPinnedShortcutsWhenDisabled.value
    }

    fun resolveCustomDnsShortcutEntry(entryId: String?): DnsHostnameEntry? {
        if (entryId.isNullOrBlank()) {
            return null
        }

        val entry =
            providers.value.firstOrNull { it.id == entryId && !it.isPredefined } ?: return null
        val shortcutId = ShortcutCatalog.getShortcutIdForDnsEntry(entry)
        return entry.takeIf { canExecuteShortcutId(shortcutId) }
    }

    private fun saveEnabledShortcutIdsInternal() {
        sharedPreferences.edit { putStringSet(KEY_ENABLED_SHORTCUT_IDS, _enabledShortcutIds.value) }
    }

    private fun saveFavoriteShortcutIdsInternal() {
        sharedPreferences.edit {
            putStringSet(
                KEY_FAVORITE_SHORTCUT_IDS,
                _favoriteShortcutIds.value
            )
        }
    }

    companion object {
        private const val KEY_ENABLED_SHORTCUT_IDS = "enabled_shortcut_ids_v1"
        private const val KEY_FAVORITE_SHORTCUT_IDS = "favorite_shortcut_ids_v1"
        private const val KEY_ALLOW_PINNED_SHORTCUTS_WHEN_DISABLED =
            "allow_pinned_shortcuts_when_disabled_v1"
    }
}
