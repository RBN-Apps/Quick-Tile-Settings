package com.rbn.qtsettings.app

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.viewmodel.MainViewModel

/** Lifecycle adapter for current system snapshots; monitoring policy stays in the legacy owner. */
class AppSystemStateObserver(context: Context, private val viewModel: MainViewModel) : DefaultLifecycleObserver {
    private val applicationContext = context.applicationContext
    private var observingQuickActionSettings = false
    private val quickActionSettingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            viewModel.refreshSystemQuickActionStates(applicationContext)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        viewModel.setApplicationContext(applicationContext)
        viewModel.checkSystemStates(applicationContext)
        viewModel.initializeVpnMonitoring()
        viewModel.initializeNetworkMonitoring()
        viewModel.refreshShortcutConfiguration()
    }

    override fun onResume(owner: LifecycleOwner) {
        viewModel.checkSystemStates(applicationContext)
        viewModel.refreshNotificationPermissionAfterSettings(applicationContext)
        viewModel.initializeNetworkMonitoring()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!observingQuickActionSettings) {
            listOf(
                Constants.PRIVATE_DNS_MODE,
                Constants.PRIVATE_DNS_SPECIFIER,
                Constants.ADB_ENABLED,
                Constants.DEVELOPMENT_SETTINGS_ENABLED
            ).forEach { settingName ->
                applicationContext.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor(settingName),
                    false,
                    quickActionSettingsObserver
                )
            }
            observingQuickActionSettings = true
        }
        viewModel.refreshSystemQuickActionStates(applicationContext)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (observingQuickActionSettings) {
            applicationContext.contentResolver.unregisterContentObserver(quickActionSettingsObserver)
            observingQuickActionSettings = false
        }
    }

}
