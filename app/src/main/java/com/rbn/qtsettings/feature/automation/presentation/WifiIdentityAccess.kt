package com.rbn.qtsettings.feature.automation.presentation

import androidx.annotation.StringRes
import com.rbn.qtsettings.R
import com.rbn.qtsettings.data.WifiIdentityAccessState

/** Copy and action for a missing Wi-Fi identity; an available identity needs no prompt. */
internal class WifiAccessPrompt(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val action: Int,
    val opensLocationSettings: Boolean
)

internal fun WifiIdentityAccessState.prompt(): WifiAccessPrompt? = when (this) {
    WifiIdentityAccessState.AVAILABLE -> null
    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED -> WifiAccessPrompt(
        R.string.wifi_ssid_permission_title, R.string.wifi_ssid_permission_description,
        R.string.wifi_ssid_permission_grant, opensLocationSettings = false)
    WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED -> WifiAccessPrompt(
        R.string.wifi_ssid_permission_title, R.string.wifi_permission_blocked_description,
        R.string.wifi_permission_open_settings, opensLocationSettings = false)
    WifiIdentityAccessState.LOCATION_SERVICES_DISABLED -> WifiAccessPrompt(
        R.string.wifi_location_services_title, R.string.wifi_location_services_description,
        R.string.wifi_location_services_open, opensLocationSettings = true)
}
