package com.rbn.qtsettings.feature.onboarding.presentation

import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod

/** UI callbacks expose intents without depending on a ViewModel or Android lifecycle. */
interface OnboardingUiActions {
    /** The page the pager settled on, so a swipe is remembered like any other move. */
    fun show(step: OnboardingStep): Unit
    fun skip(): Unit
    fun finish(): Unit
    /** Leaves the setup pages behind without granting anything. */
    fun later(): Unit
    fun chooseMethod(method: PermissionMethod): Unit
    /** Opens the system's add-tile dialog; the system decides and reports back. */
    fun addTile(tile: OnboardingTile): Unit
    fun refresh(): Unit
    fun copyCommand(command: String): Unit
    fun openDeveloperOptions(): Unit
    fun requestShizukuPermission(): Unit
    fun grantWithShizuku(): Unit
    fun grantWithRoot(): Unit
}
