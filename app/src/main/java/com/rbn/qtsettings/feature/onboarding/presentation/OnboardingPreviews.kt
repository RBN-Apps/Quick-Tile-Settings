package com.rbn.qtsettings.feature.onboarding.presentation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.feature.permissions.presentation.PermissionOperation

private val NoActions = object : OnboardingUiActions {
    override fun show(step: OnboardingStep) = Unit
    override fun skip() = Unit
    override fun later() = Unit
    override fun finish() = Unit
    override fun chooseMethod(method: PermissionMethod) = Unit
    override fun addTile(tile: OnboardingTile) = Unit
    override fun refresh() = Unit
    override fun copyCommand(command: String) = Unit
    override fun openDeveloperOptions() = Unit
    override fun requestShizukuPermission() = Unit
    override fun grantWithShizuku() = Unit
    override fun grantWithRoot() = Unit
}

private fun state(
    step: OnboardingStep,
    snapshot: PermissionSnapshot = PermissionSnapshot(),
    method: PermissionMethod? = null,
    canAddTiles: Boolean = true
) = OnboardingUiState(step, snapshot, true, PermissionOperation(), method,
    "adb shell pm grant com.rbn.qtsettings android.permission.WRITE_SECURE_SETTINGS",
    canAddTiles = canAddTiles)

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingWelcomePreview() {
    QuickTileSettingsTheme { OnboardingScreen(state(OnboardingStep.Welcome), NoActions) }
}

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 1.3f)
@Composable
private fun OnboardingTilesPreview() {
    QuickTileSettingsTheme { OnboardingScreen(state(OnboardingStep.Tiles), NoActions) }
}

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingPermissionPreview() {
    QuickTileSettingsTheme {
        OnboardingScreen(state(OnboardingStep.Permission,
            PermissionSnapshot(shizukuAvailable = true, shizukuPermissionGranted = true)), NoActions)
    }
}

@Preview(locale = "de", showBackground = true)
@Composable
private fun OnboardingAdbGrantPreview() {
    QuickTileSettingsTheme {
        OnboardingScreen(state(OnboardingStep.Grant, method = PermissionMethod.Adb), NoActions)
    }
}

@Preview(locale = "en", showBackground = true)
@Preview(locale = "de", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingDonePreview() {
    QuickTileSettingsTheme {
        OnboardingScreen(state(OnboardingStep.Done,
            PermissionSnapshot(hasWriteSecureSettings = true)), NoActions)
    }
}

/** Below Android 13 the page explains the panel instead of offering to fill it. */
@Preview(locale = "de", showBackground = true)
@Composable
private fun OnboardingDoneWithoutTileRequestPreview() {
    QuickTileSettingsTheme {
        OnboardingScreen(state(OnboardingStep.Done,
            PermissionSnapshot(hasWriteSecureSettings = true), canAddTiles = false), NoActions)
    }
}

@Preview(locale = "de", widthDp = 720, heightDp = 380)
@Composable
private fun OnboardingLandscapePreview() {
    QuickTileSettingsTheme {
        OnboardingScreen(state(OnboardingStep.Grant, PermissionSnapshot(rooted = true),
            PermissionMethod.Root), NoActions)
    }
}
