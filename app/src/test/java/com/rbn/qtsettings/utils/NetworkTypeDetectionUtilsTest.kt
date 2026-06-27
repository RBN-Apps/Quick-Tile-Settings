package com.rbn.qtsettings.utils

import android.net.NetworkCapabilities
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_MOBILE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_NONE
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
class NetworkTypeDetectionUtilsTest {

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

    private fun capabilities(vararg transports: Int): NetworkCapabilities {
        return ShadowNetworkCapabilities.newInstance().apply {
            val shadowCapabilities = Shadow.extract<ShadowNetworkCapabilities>(this)
            transports.forEach(shadowCapabilities::addTransportType)
        }
    }
}
