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
    val dnsEnableAutoRevert = prefsManager.dnsEnableAutoRevert
    val dnsAutoRevertDelaySeconds = prefsManager.dnsAutoRevertDelaySeconds


    val usbToggleEnable = prefsManager.usbToggleEnable
    val usbToggleDisable = prefsManager.usbToggleDisable
    val usbEnableAutoRevert = prefsManager.usbEnableAutoRevert
    val usbAutoRevertDelaySeconds = prefsManager.usbAutoRevertDelaySeconds

    val helpShown = prefsManager.helpShown

    private val _initialTab = MutableStateFlow(0)
    val initialTab = _initialTab.asStateFlow()


    fun setDnsToggleOff(enabled: Boolean) = prefsManager.setDnsToggleOff(enabled)
    fun setDnsToggleAuto(enabled: Boolean) = prefsManager.setDnsToggleAuto(enabled)
    fun setDnsToggleOn(enabled: Boolean) = prefsManager.setDnsToggleOn(enabled)
    fun setDnsHostname(hostname: String) = prefsManager.setDnsHostname(hostname)
    fun setDnsEnableAutoRevert(enabled: Boolean) = prefsManager.setDnsEnableAutoRevert(enabled)
    fun setDnsAutoRevertDelaySeconds(delay: Int) = prefsManager.setDnsAutoRevertDelaySeconds(delay)


    fun setUsbToggleEnable(enabled: Boolean) = prefsManager.setUsbToggleEnable(enabled)
    fun setUsbToggleDisable(enabled: Boolean) = prefsManager.setUsbToggleDisable(enabled)
    fun setUsbEnableAutoRevert(enabled: Boolean) = prefsManager.setUsbEnableAutoRevert(enabled)
    fun setUsbAutoRevertDelaySeconds(delay: Int) = prefsManager.setUsbAutoRevertDelaySeconds(delay)


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