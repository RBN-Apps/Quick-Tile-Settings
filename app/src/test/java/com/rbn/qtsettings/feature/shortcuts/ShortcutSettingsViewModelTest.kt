package com.rbn.qtsettings.feature.shortcuts

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutConfiguration
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutSettings
import com.rbn.qtsettings.feature.shortcuts.presentation.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShortcutSettingsViewModelTest {
    @Test fun dialogRestores_withoutApplyingPinnedWarning() {
        val settings = FakeSettings()
        val saved = SavedStateHandle()
        val providers = MutableStateFlow(emptyList<DnsHostnameEntry>())
        val vm = ShortcutSettingsViewModel(settings, providers, saved)
        val owners = ViewModelStore()
        owners.put("first", vm)
        try {
            vm.onAction(ShortcutSettingsAction.ShowDialog(ShortcutDialog.PinnedWarning))
            shadowOf(Looper.getMainLooper()).idle()
            val restored = ShortcutSettingsViewModel(settings, providers,
                SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))
            owners.put("restored", restored)
            assertEquals(ShortcutDialog.PinnedWarning, restored.state.value.dialog)
            restored.onAction(ShortcutSettingsAction.DismissDialog)
            assertFalse(settings.state.value.allowPinnedWhenDisabled)
            restored.onAction(ShortcutSettingsAction.SetPinnedFallback(true))
            assertTrue(settings.state.value.allowPinnedWhenDisabled)
        } finally { owners.clear() }
    }

    @Test fun rejectedActionsShowLimits_andProviderUpdatesRebuildRows() {
        val settings = FakeSettings()
        val providers = MutableStateFlow(emptyList<DnsHostnameEntry>())
        val vm = ShortcutSettingsViewModel(settings, providers, SavedStateHandle())
        val owners = ViewModelStore().apply { put("owner", vm) }
        try {
            vm.onAction(ShortcutSettingsAction.Toggle("unavailable", true))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(ShortcutDialog.Limit, vm.state.value.dialog)
            vm.onAction(ShortcutSettingsAction.Favorite("unavailable", true))
            providers.value = listOf(DnsHostnameEntry("custom", "Updated", "dns.example", false))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(ShortcutDialog.FavoriteLimit, vm.state.value.dialog)
            assertEquals(ShortcutText.Value("Updated"), vm.state.value.dnsItems.last().title)
        } finally { owners.clear() }
    }

    private class FakeSettings : ShortcutSettings {
        override val state = MutableStateFlow(ShortcutConfiguration(maxCount = 2))
        override fun setShortcutExposureEnabled(shortcutId: String, enabled: Boolean) = false
        override fun setShortcutFavorite(shortcutId: String, favorite: Boolean) = false
        override fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) { state.value = state.value.copy(allowPinnedWhenDisabled = enabled) }
        override fun refreshShortcutConfiguration() = Unit
    }
}
