package com.rbn.qtsettings.feature.dns.domain

/** Unknown/unselected current targets start at the first configured choice. */
fun nextDnsTarget(current: DnsTarget?, settings: DnsSettings): DnsTarget? {
    val choices = buildList {
        if (settings.toggleOff) add(DnsTarget.Off)
        if (settings.toggleAuto) add(DnsTarget.Automatic)
        addAll(settings.providers.filter { it.selectedForCycle }.map { it.target })
    }
    if (choices.isEmpty()) return null
    return choices[(choices.indexOf(current) + 1) % choices.size]
}
