package com.rbn.qtsettings.platform.dns

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rbn.qtsettings.feature.dns.presentation.DnsSettingsViewModel
import com.rbn.qtsettings.utils.Constants

class DnsSystemStateObserver(context: Context, private val viewModel: DnsSettingsViewModel) : DefaultLifecycleObserver {
    private val resolver = context.applicationContext.contentResolver
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = viewModel.refresh()
    }
    override fun onStart(owner: LifecycleOwner) {
        listOf(Constants.PRIVATE_DNS_MODE, Constants.PRIVATE_DNS_SPECIFIER).forEach {
            resolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        viewModel.refresh()
    }
    override fun onResume(owner: LifecycleOwner) = viewModel.refresh()
    override fun onStop(owner: LifecycleOwner) = resolver.unregisterContentObserver(observer)
}
