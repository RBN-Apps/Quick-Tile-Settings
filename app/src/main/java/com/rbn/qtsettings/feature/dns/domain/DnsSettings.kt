package com.rbn.qtsettings.feature.dns.domain

import kotlinx.coroutines.flow.StateFlow

data class DnsSettings(
    val toggleOff: Boolean = true,
    val toggleAuto: Boolean = true,
    val providers: List<DnsProvider> = emptyList(),
    val manualOrder: Boolean = false,
    val enableAutoRevert: Boolean = false,
    val autoRevertDelaySeconds: Int = 5,
    val requireUnlock: Boolean = false
)

interface DnsSettingsSource {
    val state: StateFlow<DnsSettings>
    fun read(): DnsSettings
    fun setDnsToggleOff(enabled: Boolean)
    fun setDnsToggleAuto(enabled: Boolean)
    fun setDnsEnableAutoRevert(enabled: Boolean)
    fun setDnsAutoRevertDelaySeconds(delay: Int)
    fun setDnsRequireUnlock(enabled: Boolean)
    fun setSelected(id: String, selected: Boolean)
    fun setManualOrder(manual: Boolean)
    fun reorder(ids: List<String>): Boolean
    fun add(name: String, hostname: String)
    fun edit(id: String, name: String, hostname: String)
    fun delete(id: String)
}
