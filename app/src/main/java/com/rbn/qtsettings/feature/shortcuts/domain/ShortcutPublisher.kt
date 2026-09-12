package com.rbn.qtsettings.feature.shortcuts.domain

import com.rbn.qtsettings.data.DnsHostnameEntry

interface ShortcutPublisher {
    fun maxCount(): Int
    fun publish(dnsHostnames: List<DnsHostnameEntry>, enabledShortcutIds: Set<String>, favoriteShortcutIds: Set<String>)
}
