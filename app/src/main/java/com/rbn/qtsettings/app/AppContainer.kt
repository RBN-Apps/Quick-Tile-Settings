package com.rbn.qtsettings.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.automation.domain.VpnAutomation
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.dns.domain.DnsActions
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.feature.shortcuts.presentation.ShortcutSettingsViewModel
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.platform.automation.AndroidVpnAutomationGateway
import com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource
import com.rbn.qtsettings.platform.dns.AndroidDnsSystemGateway
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway
import com.rbn.qtsettings.viewmodel.ViewModelFactory

/** Application-scoped composition root. Never retain an Activity here.
 * Legacy preferences/factory are adapters until the feature extractions (#113–#121).
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    val preferences: PreferencesManager by lazy { PreferencesManager.getInstance(applicationContext) }
    val vpnAutomation by lazy {
        VpnAutomation(AndroidVpnAutomationGateway(applicationContext))
    }
    val usbSettingsStore get() = preferences.usbStore
    val usbActions by lazy { UsbActions(AndroidUsbSystemGateway(applicationContext)) }
    val usbViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == UsbSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return UsbSettingsViewModel(usbSettingsStore, usbActions) as T
        }
    }
    val dnsSettingsStore get() = preferences.dnsStore
    val dnsActions by lazy { DnsActions(AndroidDnsSystemGateway(applicationContext)) }
    val dnsViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == DnsSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return DnsSettingsViewModel(dnsSettingsStore, dnsActions, extras.createSavedStateHandle()) as T
        }
    }
    val automationViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AutomationViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return AutomationViewModel(
                preferences.automationStore,
                ::AndroidWifiDiscoverySource
            ) as T
        }
    }
    val shortcutSettingsStore get() = preferences.shortcutStore
    val shortcutViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == ShortcutSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return ShortcutSettingsViewModel(
                shortcutSettingsStore, dnsSettingsStore.dnsHostnames, extras.createSavedStateHandle()
            ) as T
        }
    }
    val mainViewModelFactory: ViewModelFactory by lazy { ViewModelFactory(preferences) }
}
