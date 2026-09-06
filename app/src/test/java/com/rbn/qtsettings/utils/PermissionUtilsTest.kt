package com.rbn.qtsettings.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import com.rbn.qtsettings.data.WifiIdentityAccessState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLocationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PermissionUtilsTest {

    private lateinit var application: Application
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = Shadow.extract(locationManager)
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        PermissionUtils.setWifiLocationPermissionBlocked(application, false)
        shadowLocationManager.setLocationEnabled(true)
    }

    @After
    fun tearDown() {
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        PermissionUtils.setWifiLocationPermissionBlocked(application, false)
        shadowLocationManager.setLocationEnabled(true)
    }

    @Test
    fun wifiIdentityAccess_withoutPreciseLocation_requiresPermission() {
        assertEquals(
            WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_REQUIRED,
            PermissionUtils.getWifiIdentityAccessState(application)
        )
    }

    @Test
    fun wifiIdentityAccess_withPermissionAndLocationDisabled_requiresLocationServices() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowLocationManager.setLocationEnabled(false)

        assertEquals(
            WifiIdentityAccessState.LOCATION_SERVICES_DISABLED,
            PermissionUtils.getWifiIdentityAccessState(application)
        )
    }

    @Test
    fun wifiIdentityAccess_withPermissionAndLocationEnabled_isAvailable() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertEquals(
            WifiIdentityAccessState.AVAILABLE,
            PermissionUtils.getWifiIdentityAccessState(application)
        )
    }

    @Test
    fun blockedLocationPermission_recoversAfterGrantInSettings() {
        PermissionUtils.setWifiLocationPermissionBlocked(application, true)
        assertEquals(WifiIdentityAccessState.PRECISE_LOCATION_PERMISSION_BLOCKED,
            PermissionUtils.getWifiIdentityAccessState(application))
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertEquals(WifiIdentityAccessState.AVAILABLE,
            PermissionUtils.getWifiIdentityAccessState(application))
    }

    @Test
    fun bootAccess_requiresBothPreciseAndBackgroundLocation() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(PermissionUtils.hasBackgroundLocationPermission(application))
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertTrue(PermissionUtils.hasBackgroundLocationPermission(application))
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(PermissionUtils.hasBackgroundLocationPermission(application))
    }
}
