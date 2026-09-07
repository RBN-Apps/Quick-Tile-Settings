package com.rbn.qtsettings.data

import com.google.gson.annotations.SerializedName
import com.rbn.qtsettings.utils.Constants.WIFI_DNS_ACTION_DEFAULT
import java.util.UUID

data class WifiDnsRule(
    @field:SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @field:SerializedName("ssid")
    val ssid: String? = null,
    @field:SerializedName("bssid")
    val bssid: String? = null,
    @field:SerializedName("actionMode")
    val actionMode: String = WIFI_DNS_ACTION_DEFAULT,
    @field:SerializedName("dnsHostname")
    val dnsHostname: String? = null
)
