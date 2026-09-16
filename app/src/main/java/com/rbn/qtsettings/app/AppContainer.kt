package com.rbn.qtsettings.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.rbn.qtsettings.feature.automation.domain.VpnAutomation
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import com.rbn.qtsettings.feature.dns.domain.DnsActions
import com.rbn.qtsettings.feature.dns.domain.DnsAutoRevert
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.feature.shortcuts.presentation.ShortcutSettingsViewModel
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.domain.UsbAutoRevert
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.platform.automation.AndroidVpnAutomationGateway
import com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource
import com.rbn.qtsettings.platform.dns.AndroidDnsSystemGateway
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway
import com.rbn.qtsettings.feature.permissions.presentation.AppPermissionViewModel
import com.rbn.qtsettings.feature.settings.presentation.AppSettingsViewModel
import com.rbn.qtsettings.platform.permissions.AndroidPermissionGateway
import com.rbn.qtsettings.platform.backup.AndroidBackupDocuments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Application-scoped composition root. Never retain an Activity here.
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    // A pending revert belongs to the process, not to whichever tile service armed it.
    private val revertScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val stores: AppStores by lazy { AppStores.getInstance(applicationContext) }
    val vpnAutomation by lazy {
        VpnAutomation(AndroidVpnAutomationGateway(applicationContext))
    }
    val usbSettingsStore get() = stores.usb
    val usbActions by lazy { UsbActions(AndroidUsbSystemGateway(applicationContext)) }
    val usbAutoRevert by lazy { UsbAutoRevert(usbActions, revertScope) }
    val usbViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == UsbSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return UsbSettingsViewModel(usbSettingsStore, usbActions, usbAutoRevert) as T
        }
    }
    val dnsSettingsStore get() = stores.dns
    val dnsActions by lazy { DnsActions(AndroidDnsSystemGateway(applicationContext)) }
    val dnsAutoRevert by lazy { DnsAutoRevert(dnsActions, revertScope) }
    val dnsViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == DnsSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return DnsSettingsViewModel(dnsSettingsStore, dnsActions, dnsAutoRevert, extras.createSavedStateHandle()) as T
        }
    }
    val automationViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AutomationViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return AutomationViewModel(
                stores.automation,
                ::AndroidWifiDiscoverySource
            ) as T
        }
    }
    val shortcutSettingsStore get() = stores.shortcuts
    val shortcutViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == ShortcutSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return ShortcutSettingsViewModel(
                shortcutSettingsStore, dnsSettingsStore.dnsHostnames, extras.createSavedStateHandle()
            ) as T
        }
    }
    val permissionViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AppPermissionViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return AppPermissionViewModel(stores.app, AndroidPermissionGateway(applicationContext)) as T
        }
    }
    fun settingsViewModelFactory(onBackupRestored: () -> Unit) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == AppSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return AppSettingsViewModel(AndroidBackupDocuments(applicationContext, stores.backup),
                extras.createSavedStateHandle(), onBackupRestored) as T
        }
    }
}
