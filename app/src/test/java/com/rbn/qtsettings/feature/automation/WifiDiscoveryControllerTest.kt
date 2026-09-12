package com.rbn.qtsettings.feature.automation

import android.content.Context
import com.rbn.qtsettings.data.WifiIdentityAccessState
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
import com.rbn.qtsettings.feature.automation.domain.WifiDiscoverySource
import com.rbn.qtsettings.feature.automation.presentation.WifiDiscoveryController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WifiDiscoveryControllerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val source = Discovery()
    private val store = AutomationSettingsStore(context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE))
    private val controller = WifiDiscoveryController(
        { source }, { context }, store, scope, MutableStateFlow(null), {}
    )

    @After fun close() = scope.cancel()

    @Test
    fun refreshDuringScan_doesNotStartAnotherScan_andPermissionLossDiscardsResults() {
        controller.refreshWifiNetworks()
        controller.refreshWifiNetworks()
        assertEquals(1, source.scans)
        assertTrue(controller.isWifiResultsLoading.value)
        assertEquals("Campus", controller.currentWifiNetwork.value?.ssid)
        assertEquals("Campus", store.knownWifiNetworks.value.single().ssid)

        source.access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
        source.results.complete(listOf(WifiNetworkIdentity("Private network")))
        controller.refreshSystemStates()
        assertFalse(controller.isWifiResultsLoading.value)
        assertTrue(controller.scannedWifiNetworks.value.isEmpty())
        assertEquals(null, controller.currentWifiNetwork.value)
    }

    @Test
    fun unavailablePermission_requestsAccessWithoutReadingIdentityOrScanning() {
        source.access = WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED
        controller.refreshWifiNetworks()
        assertEquals(1, controller.requestWifiSsidPermission.value)
        assertEquals(0, source.scans)
        assertTrue(store.knownWifiNetworks.value.isEmpty())
        assertFalse(controller.isWifiResultsLoading.value)
    }

    @Test
    fun clearedOwner_cancelsPendingScanWithoutPublishingLateResults() {
        controller.refreshWifiNetworks()
        scope.cancel()
        source.results.complete(listOf(WifiNetworkIdentity("Late result")))
        assertTrue(controller.scannedWifiNetworks.value.isEmpty())
        assertFalse(controller.isWifiResultsLoading.value)
    }

    private class Discovery : WifiDiscoverySource {
        var access = WifiIdentityAccessState.AVAILABLE
        var scans = 0
        val results = CompletableDeferred<List<WifiNetworkIdentity>>()
        override fun accessState() = access
        override fun hasPreciseLocationPermission() = access == WifiIdentityAccessState.AVAILABLE
        override fun hasBackgroundLocationPermission() = false
        override fun current() = WifiNetworkIdentity("Campus")
        override suspend fun scanResults(): List<WifiNetworkIdentity> {
            scans++
            return results.await()
        }
    }
}
