package com.rbn.qtsettings.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.feature.usb.domain.UsbActions
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.platform.usb.AndroidUsbSystemGateway
import com.rbn.qtsettings.viewmodel.ViewModelFactory

/** Application-scoped composition root. Never retain an Activity here.
 * Legacy preferences/factory are adapters until the feature extractions (#113–#121).
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    val preferences: PreferencesManager by lazy { PreferencesManager.getInstance(applicationContext) }
    val usbSettingsStore get() = preferences.usbStore
    val usbActions by lazy { UsbActions(AndroidUsbSystemGateway(applicationContext)) }
    val usbViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == UsbSettingsViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return UsbSettingsViewModel(usbSettingsStore, usbActions) as T
        }
    }
    val mainViewModelFactory: ViewModelFactory by lazy { ViewModelFactory(preferences) }
}
