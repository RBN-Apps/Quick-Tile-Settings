package com.rbn.qtsettings.platform.usb

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rbn.qtsettings.feature.usb.presentation.UsbSettingsViewModel
import com.rbn.qtsettings.utils.Constants

class UsbSystemStateObserver(context: Context, private val viewModel: UsbSettingsViewModel) : DefaultLifecycleObserver {
    private val resolver = context.applicationContext.contentResolver
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = viewModel.refresh()
    }
    override fun onStart(owner: LifecycleOwner) {
        listOf(Constants.ADB_ENABLED, Constants.DEVELOPMENT_SETTINGS_ENABLED, Constants.ADB_WIFI_ENABLED).forEach {
            resolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        viewModel.refresh()
    }
    override fun onResume(owner: LifecycleOwner) = viewModel.refresh()
    override fun onStop(owner: LifecycleOwner) = resolver.unregisterContentObserver(observer)
}
