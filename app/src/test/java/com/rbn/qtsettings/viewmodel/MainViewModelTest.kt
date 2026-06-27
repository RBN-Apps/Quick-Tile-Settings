package com.rbn.qtsettings.viewmodel

import android.content.Context
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants.BACKGROUND_DETECTION
import com.rbn.qtsettings.utils.Constants.TILE_ONLY_DETECTION
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        prefsManager = PreferencesManager.getInstance(context)
        viewModel = MainViewModel(prefsManager)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun setInitialTab_whenCalled_thenUpdatesInitialTabFlow() {
        viewModel.setInitialTab(1)
        assertEquals(1, viewModel.initialTab.value)
    }

    @Test
    fun startAddingNewHostname_whenCalled_thenShowsDialogAndClearsEditingEntry() {
        val existing = DnsHostnameEntry(id = "edit", name = "Edit Me", hostname = "dns.edit")
        viewModel.startEditingHostname(existing)

        viewModel.startAddingNewHostname()

        assertTrue(viewModel.showHostnameEditDialog.value)
        assertNull(viewModel.editingHostname.value)
    }

    @Test
    fun startEditingAndDismissHostname_whenCalled_thenDialogStateIsManagedCorrectly() {
        val entry = DnsHostnameEntry(id = "host-1", name = "Host", hostname = "dns.host")

        viewModel.startEditingHostname(entry)
        assertTrue(viewModel.showHostnameEditDialog.value)
        assertEquals(entry, viewModel.editingHostname.value)

        viewModel.dismissHostnameEditDialog()
        assertFalse(viewModel.showHostnameEditDialog.value)
        assertNull(viewModel.editingHostname.value)
    }

    @Test
    fun setHostnamePendingDeletion_whenCalled_thenFlowIsUpdated() {
        val entry = DnsHostnameEntry(id = "delete", name = "Delete", hostname = "dns.delete")

        viewModel.setHostnamePendingDeletion(entry)

        assertEquals(entry, viewModel.hostnamePendingDeletion.value)
    }

    @Test
    fun shortcutSettings_whenFreshlyInitialized_thenNoShortcutsEnabledAndPinnedFallbackDisabled() {
        assertTrue(viewModel.enabledShortcutIds.value.isEmpty())
        assertTrue(viewModel.favoriteShortcutIds.value.isEmpty())
        assertFalse(viewModel.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun updateDnsHostnameEntrySelection_whenEntryExists_thenSelectionIsUpdated() {
        val targetId = "adguard_default"
        val before = prefsManager.dnsHostnames.value.first { it.id == targetId }
        assertTrue(before.isSelectedForCycle)

        viewModel.updateDnsHostnameEntrySelection(targetId, false)

        val after = prefsManager.dnsHostnames.value.first { it.id == targetId }
        assertFalse(after.isSelectedForCycle)
    }

    @Test
    fun editCustomDnsHostname_whenCustomEntryExists_thenNameAndHostnameAreUpdated() {
        viewModel.addCustomDnsHostname("Old Name", "old.example.com")
        val created =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "Old Name" }

        viewModel.editCustomDnsHostname(created.id, "New Name", "new.example.com")

        val updated = prefsManager.dnsHostnames.value.firstOrNull { it.id == created.id }
        assertNotNull(updated)
        assertEquals("New Name", updated?.name)
        assertEquals("new.example.com", updated?.hostname)
    }

    @Test
    fun addCustomDnsHostname_whenCalled_thenCustomShortcutRemainsDisabledByDefault() {
        viewModel.addCustomDnsHostname("New DNS", "new.example.com")

        val created =
            prefsManager.dnsHostnames.value.first { !it.isPredefined && it.name == "New DNS" }
        val shortcutId = "dns_custom_${created.id}"

        assertFalse(viewModel.enabledShortcutIds.value.contains(shortcutId))
    }

    @Test
    fun setAllowPinnedShortcutsWhenDisabled_whenCalled_thenUpdatesFlow() {
        viewModel.setAllowPinnedShortcutsWhenDisabled(true)

        assertTrue(viewModel.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun onNotificationPermissionPermanentlyDenied_whenCalled_thenSwitchesToTileOnlyAndShowsDialog() {
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionPermanentlyDenied()

        assertEquals(TILE_ONLY_DETECTION, prefsManager.getVpnDetectionMode())
        assertTrue(viewModel.showNotificationSettingsDialog.value)
    }

    @Test
    fun onNotificationPermissionResult_whenDenied_thenSwitchesToTileOnlyAndSetsStatus() {
        prefsManager.setVpnDetectionMode(BACKGROUND_DETECTION)

        viewModel.onNotificationPermissionResult(granted = false)

        assertEquals(TILE_ONLY_DETECTION, prefsManager.getVpnDetectionMode())
        assertEquals(
            "Notification permission denied. Switched to tile-only VPN detection mode.",
            viewModel.permissionGrantStatus.value
        )
    }

    @Test
    fun clearPermissionGrantStatus_whenStatusWasSet_thenClearsStatus() {
        viewModel.onNotificationPermissionResult(granted = false)
        assertNotNull(viewModel.permissionGrantStatus.value)

        viewModel.clearPermissionGrantStatus()

        assertNull(viewModel.permissionGrantStatus.value)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
