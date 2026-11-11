package com.rbn.qtsettings.feature.onboarding.presentation

import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.feature.permissions.domain.orderedMethods
import com.rbn.qtsettings.feature.permissions.presentation.PermissionOperation

/** Rendering snapshot for the first run; permission truth still comes from the permission owner. */
data class OnboardingUiState(
    val step: OnboardingStep,
    val permission: PermissionSnapshot,
    val initialized: Boolean,
    val operation: PermissionOperation,
    val method: PermissionMethod?,
    val grantCommand: String,
    val feedback: String? = null,
    val tilesInPanel: Set<OnboardingTile> = emptySet(),
    /** Android 13 and up can offer to place a tile; older releases only explain where to drag it. */
    val canAddTiles: Boolean = false,
    /** Off when the device has its animations turned down, so nothing loops forever. */
    val decorativeMotion: Boolean = true
) {
    val granted get() = permission.hasWriteSecureSettings
    val busy get() = !initialized || operation.checking || operation.granting
    /** Swiping past the method list without picking one lands on the recommended way. */
    val setupMethod get() = method ?: permission.orderedMethods().first()
}
