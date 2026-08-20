package com.rbn.qtsettings.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class DnsHostnameEntry(
    @field:SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @field:SerializedName("name")
    val name: String,
    @field:SerializedName("hostname")
    val hostname: String,
    @field:SerializedName("isPredefined")
    val isPredefined: Boolean = false,
    @field:SerializedName("isSelectedForCycle")
    var isSelectedForCycle: Boolean = true,
    @field:SerializedName("descriptionResId")
    val descriptionResId: Int? = null
)
