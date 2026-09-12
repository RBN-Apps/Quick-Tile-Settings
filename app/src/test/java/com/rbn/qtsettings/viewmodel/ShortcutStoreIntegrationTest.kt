package com.rbn.qtsettings.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortcutStoreIntegrationTest : SettingsIntegrationFixture() {

    @Test
    fun shortcutSettings_whenFreshlyInitialized_thenNoShortcutsEnabledAndPinnedFallbackDisabled() {
        assertTrue(prefsManager.shortcuts.enabledShortcutIds.value.isEmpty())
        assertTrue(prefsManager.shortcuts.favoriteShortcutIds.value.isEmpty())
        assertFalse(prefsManager.shortcuts.allowPinnedShortcutsWhenDisabled.value)
    }

    @Test
    fun addCustomDnsHostname_whenCalled_thenCustomShortcutRemainsDisabledByDefault() {
        prefsManager.dns.addCustomDnsHostname("New DNS", "new.example.com")

        val created =
            prefsManager.dns.dnsHostnames.value.first { !it.isPredefined && it.name == "New DNS" }
        val shortcutId = "dns_custom_${created.id}"

        assertFalse(prefsManager.shortcuts.enabledShortcutIds.value.contains(shortcutId))
    }

    @Test
    fun setAllowPinnedShortcutsWhenDisabled_whenCalled_thenUpdatesFlow() {
        prefsManager.shortcuts.setAllowPinnedShortcutsWhenDisabled(true)

        assertTrue(prefsManager.shortcuts.allowPinnedShortcutsWhenDisabled.value)
    }
}
