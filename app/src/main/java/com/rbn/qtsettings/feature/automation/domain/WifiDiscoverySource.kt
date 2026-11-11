package com.rbn.qtsettings.feature.automation.domain

import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity

/** Framework permissions and redacted WifiInfo are interpreted at this boundary. */
interface WifiDiscoverySource {
    fun accessState(): WifiIdentityAccessState
    fun hasPreciseLocationPermission(): Boolean
    fun hasBackgroundLocationPermission(): Boolean
    fun current(): WifiNetworkIdentity?
    suspend fun scanResults(): List<WifiNetworkIdentity>
}
