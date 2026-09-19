package com.rbn.qtsettings.app

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppControlPermissionObserverTest {
    private val permissions = MutableStateFlow(PermissionSnapshot())
    private val refreshes = mutableListOf<Boolean>()
    private val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }

    init {
        owner.lifecycle.addObserver(AppControlPermissionObserver(permissions) {
            refreshes += permissions.value.hasWriteSecureSettings
        })
    }

    private fun moveTo(state: Lifecycle.State) {
        owner.lifecycle.currentState = state
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun publish(snapshot: PermissionSnapshot) {
        permissions.value = snapshot
        shadowOf(Looper.getMainLooper()).idle()
    }

    @After fun destroy() = moveTo(Lifecycle.State.DESTROYED)

    @Test fun grantDuringOnboardingAndRevocation_refreshFeaturesWithoutAnotherResume() {
        moveTo(Lifecycle.State.RESUMED)
        assertEquals(listOf(false), refreshes)

        // The permission page discovers a grant while the same activity remains resumed.
        publish(permissions.value.copy(hasWriteSecureSettings = true))
        assertEquals(listOf(false, true), refreshes)
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)

        publish(permissions.value.copy(hasWriteSecureSettings = false))
        assertEquals(listOf(false, true, false), refreshes)
    }

    @Test fun quietChecksAndUnrelatedCapabilities_doNotRepeatedlyRefreshFeatures() {
        moveTo(Lifecycle.State.RESUMED)
        publish(permissions.value.copy(rooted = true, shizukuAvailable = true))
        publish(permissions.value.copy(shizukuPermissionGranted = true))
        publish(permissions.value.copy(developerOptionsEnabled = true))
        publish(permissions.value)
        assertEquals(listOf(false), refreshes)
    }

    @Test fun stoppedActivityCatchesUpOnStart_andDestroyedActivityStopsCollecting() {
        moveTo(Lifecycle.State.RESUMED)
        moveTo(Lifecycle.State.CREATED)
        publish(permissions.value.copy(hasWriteSecureSettings = true))
        assertEquals(listOf(false), refreshes)

        moveTo(Lifecycle.State.RESUMED)
        assertEquals(listOf(false, true), refreshes)
        moveTo(Lifecycle.State.DESTROYED)
        publish(permissions.value.copy(hasWriteSecureSettings = false))
        assertEquals(listOf(false, true), refreshes)
    }
}
