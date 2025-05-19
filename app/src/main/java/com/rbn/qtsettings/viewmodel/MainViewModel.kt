package com.rbn.qtsettings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.data.DnsHostnameEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val prefsManager: PreferencesManager) : ViewModel() {

    val dnsToggleOff = prefsManager.dnsToggleOff
    val dnsToggleAuto = prefsManager.dnsToggleAuto
    val dnsHostnames = prefsManager.dnsHostnames
    val dnsEnableAutoRevert = prefsManager.dnsEnableAutoRevert
    val dnsAutoRevertDelaySeconds = prefsManager.dnsAutoRevertDelaySeconds


    val usbToggleEnable = prefsManager.usbToggleEnable
    val usbToggleDisable = prefsManager.usbToggleDisable
    val usbEnableAutoRevert = prefsManager.usbEnableAutoRevert
    val usbAutoRevertDelaySeconds = prefsManager.usbAutoRevertDelaySeconds

    val helpShown = prefsManager.helpShown

    private val _initialTab = MutableStateFlow(0)
    val initialTab = _initialTab.asStateFlow()

    private val _showHostnameEditDialog = MutableStateFlow(false)
    val showHostnameEditDialog = _showHostnameEditDialog.asStateFlow()

    private val _editingHostname = MutableStateFlow<DnsHostnameEntry?>(null)
    val editingHostname = _editingHostname.asStateFlow()

    private val _hostnamePendingDeletion = MutableStateFlow<DnsHostnameEntry?>(null)
    val hostnamePendingDeletion = _hostnamePendingDeletion.asStateFlow()


    fun setDnsToggleOff(enabled: Boolean) = prefsManager.setDnsToggleOff(enabled)
    fun setDnsToggleAuto(enabled: Boolean) = prefsManager.setDnsToggleAuto(enabled)
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

    fun updateDnsHostnameEntrySelection(id: String, isSelected: Boolean) {
        val entry = dnsHostnames.value.find { it.id == id }
        entry?.let {
            prefsManager.updateDnsHostnameEntry(it.copy(isSelectedForCycle = isSelected))
        }
    }

    fun addCustomDnsHostname(name: String, hostnameValue: String) {
        prefsManager.addCustomDnsHostname(name, hostnameValue)
    }

    fun editCustomDnsHostname(id: String, newName: String, newHostnameValue: String) {
        val entry = dnsHostnames.value.find { it.id == id && !it.isPredefined }
        entry?.let {
            prefsManager.updateDnsHostnameEntry(it.copy(name = newName, hostname = newHostnameValue))
        }
    }

    fun deleteCustomDnsHostname(id: String) {
        prefsManager.deleteCustomDnsHostname(id)
    }

    fun startAddingNewHostname() { _editingHostname.value = null; _showHostnameEditDialog.value = true }
    fun startEditingHostname(entry: DnsHostnameEntry) { _editingHostname.value = entry; _showHostnameEditDialog.value = true }
    fun dismissHostnameEditDialog() { _showHostnameEditDialog.value = false; _editingHostname.value = null }
    fun setHostnamePendingDeletion(entry: DnsHostnameEntry?) { _hostnamePendingDeletion.value = entry }
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