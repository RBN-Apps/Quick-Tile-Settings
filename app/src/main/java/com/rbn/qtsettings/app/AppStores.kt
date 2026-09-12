package com.rbn.qtsettings.app

import android.content.Context
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
import com.rbn.qtsettings.feature.dns.data.DnsSettingsStore
import com.rbn.qtsettings.feature.settings.data.AppSettingsStore
import com.rbn.qtsettings.feature.settings.data.SettingsBackupStore
import com.rbn.qtsettings.feature.shortcuts.data.ShortcutSettingsStore
import com.rbn.qtsettings.feature.usb.data.UsbSettingsStore
import com.rbn.qtsettings.platform.shortcuts.AndroidShortcutPublisher

/** One process-wide persistence graph, shared by the UI, tiles, services and legacy adapters. */
class AppStores private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
    val app = AppSettingsStore(preferences)
    val dns = DnsSettingsStore(preferences)
    val usb = UsbSettingsStore(preferences)
    val automation = AutomationSettingsStore(preferences)
    val shortcuts = ShortcutSettingsStore(preferences, dns.dnsHostnames, AndroidShortcutPublisher(context))
    val backup = SettingsBackupStore(preferences, dns, usb, automation, shortcuts)
    internal val legacyPreferences by lazy { PreferencesManager(this) }
    init { dns.onProvidersChanged = shortcuts::reconcileProviders }

    companion object {
        @Volatile private var INSTANCE: AppStores? = null
        fun getInstance(context: Context): AppStores = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppStores(context.applicationContext).also { INSTANCE = it }
        }
    }
}
