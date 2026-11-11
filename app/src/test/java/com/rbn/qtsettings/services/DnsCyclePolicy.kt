package com.rbn.qtsettings.services

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.dns.data.toDomain
import com.rbn.qtsettings.feature.dns.data.toPersisted
import com.rbn.qtsettings.feature.dns.data.toProvider
import com.rbn.qtsettings.feature.dns.domain.DnsSettings
import com.rbn.qtsettings.feature.dns.domain.nextDnsTarget

internal data class DnsCycleState(
    val mode: String,
    val hostname: String? = null
)

internal object DnsCyclePolicy {
    fun nextState(
        currentMode: String,
        currentHostname: String?,
        includeOff: Boolean,
        includeAuto: Boolean,
        hostnames: List<DnsHostnameEntry>
    ): DnsCycleState? {
        val target = nextDnsTarget(
            PrivateDnsTarget(currentMode, currentHostname).toDomain(),
            DnsSettings(toggleOff = includeOff, toggleAuto = includeAuto,
                providers = hostnames.map { it.toProvider().copy(selectedForCycle = true) })
        )?.toPersisted() ?: return null
        return DnsCycleState(target.mode, target.hostname)
    }
}
