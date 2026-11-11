package com.rbn.qtsettings.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingViewModel
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class OnboardingViewModelTest {
    private class Settings : AppSettings {
        override val helpShown = MutableStateFlow(false)
        override fun setHelpShown(shown: Boolean) { helpShown.value = shown }
        override val onboardingCompleted = MutableStateFlow(false)
        override fun setOnboardingCompleted(completed: Boolean) { onboardingCompleted.value = completed }
    }
    private val settings = Settings()
    private val handle = SavedStateHandle()
    private val viewModel = OnboardingViewModel(settings, handle)

    @Test fun pickingAMethod_opensItsSetupPageAndKeepsTheChoice() {
        viewModel.show(OnboardingStep.Permission)
        viewModel.choose(PermissionMethod.Shizuku)
        assertEquals(OnboardingStep.Grant, viewModel.step.value)
        assertEquals(PermissionMethod.Shizuku, viewModel.method.value)
    }

    @Test fun processDeath_restoresThePageAndTheChosenMethod() {
        viewModel.show(OnboardingStep.Tiles)
        viewModel.choose(PermissionMethod.Root)
        val restored = OnboardingViewModel(settings, handle)
        assertEquals(OnboardingStep.Grant, restored.step.value)
        assertEquals(PermissionMethod.Root, restored.method.value)
    }

    /** A saved name that no longer exists must not take the whole first run down with it. */
    @Test fun unknownSavedNames_fallBackToTheFirstPage() {
        handle["onboarding_step"] = "RetiredPage"
        handle["onboarding_method"] = "RetiredMethod"
        val restored = OnboardingViewModel(settings, handle)
        assertEquals(OnboardingStep.Welcome, restored.step.value)
        assertNull(restored.method.value)
    }

    /** Swiping reports every page it settles on, including a return to one already recorded. */
    @Test fun repeatedShow_recordsThePageWithoutSideEffects() {
        viewModel.show(OnboardingStep.Tiles)
        viewModel.show(OnboardingStep.Tiles)
        assertEquals(OnboardingStep.Tiles, viewModel.step.value)
        assertEquals("Tiles", handle.get<String>("onboarding_step"))
        viewModel.show(OnboardingStep.Welcome)
        assertEquals(OnboardingStep.Welcome, viewModel.step.value)
    }

    @Test fun deferringTheSetup_leavesBothSetupPagesAtOnce() {
        viewModel.show(OnboardingStep.Permission)
        viewModel.skipSetup()
        assertEquals(OnboardingStep.Done, viewModel.step.value)
        assertFalse(settings.onboardingCompleted.value)
    }

    @Test fun arrivingPermission_endsTheSetupPageWithoutTouchingLaterPages() {
        viewModel.choose(PermissionMethod.Adb)
        viewModel.onPermissionGranted()
        assertEquals(OnboardingStep.Done, viewModel.step.value)
        viewModel.show(OnboardingStep.Tiles)
        viewModel.onPermissionGranted()
        assertEquals(OnboardingStep.Tiles, viewModel.step.value)
    }

    /** The system owns the panel, so a tile is only marked once its dialog reports back. */
    @Test fun tileResults_recordWhatTheSystemReportedAndSurviveProcessDeath() {
        viewModel.onTileResult(OnboardingTile.Dns, present = true)
        viewModel.onTileResult(OnboardingTile.Usb, present = false)
        assertEquals(setOf(OnboardingTile.Dns), viewModel.tilesInPanel.value)
        val restored = OnboardingViewModel(settings, handle)
        assertEquals(setOf(OnboardingTile.Dns), restored.tilesInPanel.value)
        restored.onTileResult(OnboardingTile.Dns, present = false)
        assertEquals(emptySet<OnboardingTile>(), restored.tilesInPanel.value)
    }

    @Test fun finishing_alsoRetiresTheOneTimePermissionPage() {
        viewModel.finish()
        assertTrue(settings.onboardingCompleted.value)
        assertTrue(settings.helpShown.value)
    }

    @Test fun restart_reopensTheFlowAtItsFirstPageWithoutAMethodOrTiles() {
        viewModel.choose(PermissionMethod.Root)
        viewModel.onTileResult(OnboardingTile.Dns, present = true)
        viewModel.finish()
        viewModel.restart()
        assertFalse(settings.onboardingCompleted.value)
        assertEquals(OnboardingStep.Welcome, viewModel.step.value)
        assertNull(viewModel.method.value)
        assertEquals(emptySet<OnboardingTile>(), viewModel.tilesInPanel.value)
    }
}
