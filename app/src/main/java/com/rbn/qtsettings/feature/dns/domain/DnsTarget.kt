package com.rbn.qtsettings.feature.dns.domain

/** A DNS choice, independent of Android settings strings and resources. */
sealed interface DnsTarget {
    data object Off : DnsTarget
    data object Automatic : DnsTarget
    data class Hostname(val value: String) : DnsTarget
}

data class DnsProvider(
    val id: String,
    val name: String,
    val target: DnsTarget.Hostname,
    val predefined: Boolean,
    val selectedForCycle: Boolean
)
