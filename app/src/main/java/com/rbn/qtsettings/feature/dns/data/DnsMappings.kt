package com.rbn.qtsettings.feature.dns.data

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.dns.domain.DnsProvider
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON

internal fun DnsHostnameEntry.toProvider() = DnsProvider(
    id, name, DnsTarget.Hostname(hostname), isPredefined, isSelectedForCycle
)

fun PrivateDnsTarget.toDomain(): DnsTarget? = when (mode) {
    DNS_MODE_OFF -> DnsTarget.Off
    DNS_MODE_AUTO -> DnsTarget.Automatic
    DNS_MODE_ON -> hostname?.let(DnsTarget::Hostname)
    else -> null
}

fun DnsTarget.toPersisted(): PrivateDnsTarget = when (this) {
    DnsTarget.Off -> PrivateDnsTarget(DNS_MODE_OFF)
    DnsTarget.Automatic -> PrivateDnsTarget(DNS_MODE_AUTO)
    is DnsTarget.Hostname -> PrivateDnsTarget(DNS_MODE_ON, value)
}
