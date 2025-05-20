package com.rbn.qtsettings.data

import java.util.UUID

data class DnsHostnameEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val isPredefined: Boolean = false,
    var isSelectedForCycle: Boolean = true,
    val descriptionResId: Int? = null
)