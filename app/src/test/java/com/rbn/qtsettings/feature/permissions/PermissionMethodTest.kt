package com.rbn.qtsettings.feature.permissions

import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod.*
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.feature.permissions.domain.orderedMethods
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionMethodTest {
    @Test fun recommendationPreservesAllFourLegacyCapabilityCombinations() {
        val cases = listOf(
            PermissionSnapshot(rooted = true, shizukuAvailable = true) to listOf(Root, Shizuku, Adb),
            PermissionSnapshot(rooted = true) to listOf(Root, Adb, Shizuku),
            PermissionSnapshot(shizukuAvailable = true) to listOf(Shizuku, Adb, Root),
            PermissionSnapshot() to listOf(Adb, Shizuku, Root)
        )
        for ((snapshot, expected) in cases) {
            assertEquals(expected, snapshot.orderedMethods())
            // App authorization changes the Shizuku action, not its recommendation priority.
            assertEquals(expected, snapshot.copy(shizukuPermissionGranted = true).orderedMethods())
        }
    }
}
