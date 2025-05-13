package com.rbn.qtsettings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbn.qtsettings.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val prefsManager: PreferencesManager) : ViewModel() {

    val dnsToggleOff = prefsManager.dnsToggleOff
    val dnsToggleAuto = prefsManager.dnsToggleAuto
    val dnsToggleOn = prefsManager.dnsToggleOn
    val dnsHostname = prefsManager.dnsHostname

    val usbToggleEnable = prefsManager.usbToggleEnable
    val usbToggleDisable = prefsManager.usbToggleDisable

    val helpShown = prefsManager.helpShown

    private val _initialTab = MutableStateFlow(0)
    val initialTab = _initialTab.asStateFlow()


    fun setDnsToggleOff(enabled: Boolean) = prefsManager.setDnsToggleOff(enabled)
    fun setDnsToggleAuto(enabled: Boolean) = prefsManager.setDnsToggleAuto(enabled)
    fun setDnsToggleOn(enabled: Boolean) = prefsManager.setDnsToggleOn(enabled)
    fun setDnsHostname(hostname: String) = prefsManager.setDnsHostname(hostname)

    fun setUsbToggleEnable(enabled: Boolean) = prefsManager.setUsbToggleEnable(enabled)
    fun setUsbToggleDisable(enabled: Boolean) = prefsManager.setUsbToggleDisable(enabled)

    fun setHelpShown(shown: Boolean) = prefsManager.setHelpShown(shown)

    fun setInitialTab(tabIndex: Int) {
        _initialTab.value = tabIndex
    }
}

class ViewModelFactory(private val prefsManager: PreferencesManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}