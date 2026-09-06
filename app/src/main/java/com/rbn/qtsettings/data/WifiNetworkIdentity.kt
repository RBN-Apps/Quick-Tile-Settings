package com.rbn.qtsettings.data

import com.google.gson.annotations.SerializedName

data class WifiNetworkIdentity(
    @field:SerializedName("ssid")
    val ssid: String? = null,
    @field:SerializedName("bssid")
    val bssid: String? = null
) {
    val stableKey: String
        get() = "${ssid.orEmpty()}|${bssid.orEmpty()}"
}

data class KnownWifiNetwork(
    @field:SerializedName("ssid")
    val ssid: String? = null,
    @field:SerializedName("bssid")
    val bssid: String? = null,
    @field:SerializedName("lastSeenEpochMillis")
    val lastSeenEpochMillis: Long = 0L
) {
    val identity: WifiNetworkIdentity
        get() = WifiNetworkIdentity(ssid = ssid, bssid = bssid)
}
