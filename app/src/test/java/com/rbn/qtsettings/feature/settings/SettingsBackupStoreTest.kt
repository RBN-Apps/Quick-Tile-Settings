package com.rbn.qtsettings.feature.settings

import android.content.Context
import android.net.Uri
import com.google.gson.JsonParser
import com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
import com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
import com.rbn.qtsettings.feature.settings.data.AppSettingsStore
import com.rbn.qtsettings.feature.settings.data.SettingsBackupStore
import com.rbn.qtsettings.feature.shortcuts.ShortcutSettingsStoreTest.RecordingPublisher
import com.rbn.qtsettings.feature.shortcuts.data.ShortcutSettingsStore
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.platform.backup.AndroidBackupDocuments
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsBackupStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("backup-owner-test", Context.MODE_PRIVATE)
    private val publisher = RecordingPublisher()
    private val dns = DnsSettingsStore(preferences)
    private val usb = UsbSettingsStore(preferences)
    private val automation = AutomationSettingsStore(preferences)
    private val shortcuts = ShortcutSettingsStore(preferences, dns.dnsHostnames, publisher)
    private val backup = SettingsBackupStore(preferences, dns, usb, automation, shortcuts)

    @Test fun invalidDocument_changesNeitherPersistentDataNorOwnersOrPublishing() {
        dns.addCustomDnsHostname("Home", "dns.example.com")
        usb.setRequireUnlock(true)
        automation.setNetworkTypeDetectionEnabled(true)
        shortcuts.setShortcutExposureEnabled(ShortcutCatalog.SHORTCUT_ID_DNS_OFF, true)
        val before = preferences.all.toMap()
        val beforeDns = dns.state.value
        val beforeUsb = usb.state.value
        val beforeShortcuts = shortcuts.state.value
        val publishes = publisher.history.size
        for (invalid in listOf("not json", "{}", backup.exportSettingsBackupJson().replace("\"schemaVersion\":1", "\"schemaVersion\":999"))) {
            assertThrows(IllegalArgumentException::class.java) { backup.restoreSettingsBackupJson(invalid) }
            assertEquals(before, preferences.all)
            assertEquals(beforeDns, dns.state.value)
            assertEquals(beforeUsb, usb.state.value)
            assertEquals(beforeShortcuts, shortcuts.state.value)
            assertTrue(automation.networkTypeDetectionEnabled.value)
            assertEquals(publishes, publisher.history.size)
        }
    }

    @Test fun documentRoundtrip_restoresOwnersAndPublisher_andSurvivesStoreRestart() {
        val app = AppSettingsStore(preferences)
        app.setHelpShown(true)
        dns.addCustomDnsHostname("Home", "dns.example.com")
        usb.setRequireUnlock(true)
        automation.setDnsStateOnMobile("off")
        val id = ShortcutCatalog.getShortcutIdForDnsEntry(dns.dnsHostnames.value.last { !it.isPredefined })
        shortcuts.setShortcutExposureEnabled(id, true)
        shortcuts.setShortcutFavorite(id, true)
        val file = File.createTempFile("backup-roundtrip", ".json", context.cacheDir)
        val documents = AndroidBackupDocuments(context, backup)
        try {
            documents.export(Uri.fromFile(file).toString())
            assertFalse(JsonParser.parseString(file.readText()).asJsonObject.has("helpShown"))
            dns.deleteCustomDnsHostname(dns.dnsHostnames.value.last { !it.isPredefined }.id)
            usb.setRequireUnlock(false)
            automation.setDnsStateOnMobile("auto")
            shortcuts.setShortcutExposureEnabled(id, false)
            documents.restore(Uri.fromFile(file).toString())
            assertTrue(usb.state.value.requireUnlock)
            assertEquals("off", automation.dnsStateOnMobile.value)
            assertEquals(setOf(id), shortcuts.state.value.enabledIds)
            assertEquals(setOf(id), publisher.history.last().first)
            assertTrue(publisher.entries.any { it.hostname == "dns.example.com" })
            assertTrue(AppSettingsStore(preferences).helpShown.value)
            val restartedDns = DnsSettingsStore(preferences)
            assertEquals(dns.dnsHostnames.value, restartedDns.dnsHostnames.value)
            assertEquals(usb.state.value, UsbSettingsStore(preferences).state.value)
            assertEquals(shortcuts.state.value, ShortcutSettingsStore(preferences, restartedDns.dnsHostnames, publisher).state.value)
        } finally { file.delete() }
    }
}
