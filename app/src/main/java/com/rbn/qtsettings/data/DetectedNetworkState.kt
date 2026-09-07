package com.rbn.qtsettings.data

import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE

/**
 * The physical network currently used by the device.
 *
 * [wifiSsid] and [wifiBssid] are deliberately nullable. Android redacts them when location access
 * is unavailable, and an unknown identity must never be treated as an unmatched network.
 */
data class DetectedNetworkState(
    val networkType: String = NETWORK_TYPE_NONE,
    val wifiSsid: String? = null,
    val wifiBssid: String? = null
)
