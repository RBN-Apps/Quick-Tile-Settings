package com.rbn.qtsettings.feature.shortcuts

import android.content.Context
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.ShortcutSettingsBackup
import com.rbn.qtsettings.feature.shortcuts.data.ShortcutSettingsStore
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutPublisher
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ShortcutSettingsStoreTest {
    private val custom = DnsHostnameEntry("custom", "Home", "dns.example", false)
    private val providers = MutableStateFlow(listOf(custom))
    private val publisher = RecordingPublisher()
    private val prefs get() = RuntimeEnvironment.getApplication().getSharedPreferences("shortcut-owner-test", Context.MODE_PRIVATE)
    private lateinit var store: ShortcutSettingsStore

    @Before fun setup() {
        prefs.edit().clear().commit()
        store = ShortcutSettingsStore(prefs, providers, publisher)
    }

    @Test fun limitAndFavoriteRejection_doNotPublishOrPersistPartialChanges() {
        val first = ShortcutCatalog.SHORTCUT_ID_DNS_OFF
        val second = ShortcutCatalog.SHORTCUT_ID_DNS_AUTO
        assertFalse(store.setShortcutFavorite(first, true))
        assertTrue(store.setShortcutExposureEnabled(first, true))
        assertTrue(store.setShortcutFavorite(first, true))
        assertTrue(store.setShortcutExposureEnabled(second, true))
        val before = store.state.value
        val publishes = publisher.history.size
        assertFalse(store.setShortcutExposureEnabled(ShortcutCatalog.SHORTCUT_ID_USB_ON, true))
        assertEquals(before, store.state.value)
        assertEquals(publishes, publisher.history.size)
        assertTrue(store.setShortcutExposureEnabled(first, false))
        assertTrue(store.state.value.favoriteIds.isEmpty())
        assertEquals(setOf(second), prefs.getStringSet("enabled_shortcut_ids_v1", null))
    }

    @Test fun providerRenameAndDeletion_reconcilePublishingAndExecution() {
        val id = ShortcutCatalog.getShortcutIdForDnsEntry(custom)
        store.setShortcutExposureEnabled(id, true)
        store.setShortcutFavorite(id, true)
        providers.value = listOf(custom.copy(name = "Renamed", hostname = "new.example"))
        store.reconcileProviders()
        assertEquals("Renamed", publisher.entries.single().name)
        assertEquals("new.example", store.resolveCustomDnsShortcutEntry(custom.id)?.hostname)
        providers.value = emptyList()
        store.reconcileProviders()
        assertTrue(store.state.value.enabledIds.isEmpty())
        assertTrue(store.state.value.favoriteIds.isEmpty())
        store.setAllowPinnedShortcutsWhenDisabled(true)
        assertFalse(store.canExecuteShortcutId(id))
        assertNull(store.resolveCustomDnsShortcutEntry(custom.id))
    }

    @Test fun restartAndRestore_keepStableIdsPinnedPolicyAndFavoriteOrder() {
        val backup = ShortcutSettingsBackup(setOf("dns_off", "dns_custom_custom"), setOf("dns_custom_custom"), true)
        prefs.edit().also { store.writeTo(it, backup) }.commit()
        store.restore(backup)
        val restored = ShortcutSettingsStore(prefs, providers, publisher)
        assertEquals(store.state.value, restored.state.value)
        assertEquals(setOf("dyn_dns_off", "dns_custom_custom"), restored.state.value.enabledIds)
        assertEquals(listOf("dns_custom_custom", "dyn_dns_off"),
            ShortcutCatalog.getOrderedShortcutIds(providers.value, restored.state.value.favoriteIds)
                .filter { it in restored.state.value.enabledIds })
        assertTrue(restored.canExecuteShortcutId(ShortcutCatalog.SHORTCUT_ID_USB_ON))
        restored.setAllowPinnedShortcutsWhenDisabled(false)
        assertFalse(restored.canExecuteShortcutId(ShortcutCatalog.SHORTCUT_ID_USB_ON))
    }

    internal class RecordingPublisher : ShortcutPublisher {
        var entries = emptyList<DnsHostnameEntry>()
        val history = mutableListOf<Pair<Set<String>, Set<String>>>()
        override fun maxCount() = 2
        override fun publish(dnsHostnames: List<DnsHostnameEntry>, enabledShortcutIds: Set<String>, favoriteShortcutIds: Set<String>) {
            entries = dnsHostnames
            history += enabledShortcutIds to favoriteShortcutIds
        }
    }
}
