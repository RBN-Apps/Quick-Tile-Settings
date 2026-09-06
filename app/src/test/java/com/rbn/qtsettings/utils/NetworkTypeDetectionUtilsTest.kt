package com.rbn.qtsettings.utils

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowWifiInfo

@RunWith(RobolectricTestRunner::class)
class NetworkTypeDetectionUtilsTest {

    @Test
    @Config(sdk = [29, 30])
    fun legacyAndroid_readsWifiIdentityWithoutTransportInfo() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(app.getSystemService(LocationManager::class.java)).setLocationEnabled(true)
        val wifiInfo = ShadowWifiInfo.newInstance()
        shadowOf(wifiInfo).setSSID("\"Campus\"")
        shadowOf(wifiInfo).setBSSID("aa:bb:cc:dd:ee:ff")
        shadowOf(app.getSystemService(Context.WIFI_SERVICE) as WifiManager).setConnectionInfo(wifiInfo)
        var detected: WifiNetworkIdentity? = null
        val callback = NetworkTypeDetectionUtils.createNetworkStateCallback(app) {
            detected = WifiNetworkIdentity(it.wifiSsid, it.wifiBssid)
        }
        NetworkTypeDetectionUtils.registerNetworkTypeCallback(app, callback)
        try {
            callback.onCapabilitiesChanged(
                ShadowNetwork.newInstance(123), capabilities(NetworkCapabilities.TRANSPORT_WIFI)
            )
            assertEquals(WifiNetworkIdentity("\"Campus\"", "AA:BB:CC:DD:EE:FF"), detected)
        } finally {
            NetworkTypeDetectionUtils.unregisterNetworkTypeCallback(app, callback)
            shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Test
    fun networkTypeFromCapabilities_prefersPhysicalTransportOnVpnNetwork() {
        val vpnOverWifi = capabilities(
            NetworkCapabilities.TRANSPORT_VPN,
            NetworkCapabilities.TRANSPORT_WIFI
        )

        val type = NetworkTypeDetectionUtils.getNetworkTypeFromCapabilities(vpnOverWifi)

        assertEquals(NETWORK_TYPE_WIFI, type)
    }

    @Test
    fun bestNetworkTypeFromCapabilities_fallsBackToUnderlyingWifiWhenVpnHidesTransport() {
        val type = NetworkTypeDetectionUtils.getBestNetworkTypeFromCapabilities(
            listOf(
                capabilities(NetworkCapabilities.TRANSPORT_VPN),
                capabilities(NetworkCapabilities.TRANSPORT_WIFI)
            )
        )

        assertEquals(NETWORK_TYPE_WIFI, type)
    }

    @Test
    fun bestNetworkTypeFromCapabilities_returnsMobileWhenOnlyUnderlyingMobileIsAvailable() {
        val type = NetworkTypeDetectionUtils.getBestNetworkTypeFromCapabilities(
            listOf(
                capabilities(NetworkCapabilities.TRANSPORT_VPN),
                capabilities(NetworkCapabilities.TRANSPORT_CELLULAR)
            )
        )

        assertEquals(NETWORK_TYPE_MOBILE, type)
    }

    @Test
    fun bestNetworkTypeFromCapabilities_returnsNoneWithoutPhysicalNetwork() {
        val type = NetworkTypeDetectionUtils.getBestNetworkTypeFromCapabilities(
            listOf(capabilities(NetworkCapabilities.TRANSPORT_VPN))
        )

        assertEquals(NETWORK_TYPE_NONE, type)
    }

    @Test
    fun currentNetworkType_withoutWifiRules_prefersActiveMobileOverTrackedWifi() {
        val type = NetworkTypeDetectionUtils.resolveCurrentNetworkType(
            activeCapabilities = capabilities(NetworkCapabilities.TRANSPORT_CELLULAR),
            trackedCapabilities = listOf(capabilities(NetworkCapabilities.TRANSPORT_WIFI)),
            preferTrackedWifi = false
        )

        assertEquals(NETWORK_TYPE_MOBILE, type)
    }

    @Test
    fun currentNetworkType_withWifiRules_prefersTrackedWifiOverActiveMobile() {
        val type = NetworkTypeDetectionUtils.resolveCurrentNetworkType(
            activeCapabilities = capabilities(NetworkCapabilities.TRANSPORT_CELLULAR),
            trackedCapabilities = listOf(capabilities(NetworkCapabilities.TRANSPORT_WIFI)),
            preferTrackedWifi = true
        )

        assertEquals(NETWORK_TYPE_WIFI, type)
    }

    private fun capabilities(vararg transports: Int): NetworkCapabilities {
        return ShadowNetworkCapabilities.newInstance().apply {
            val shadowCapabilities = Shadow.extract<ShadowNetworkCapabilities>(this)
            transports.forEach(shadowCapabilities::addTransportType)
        }
    }
}
