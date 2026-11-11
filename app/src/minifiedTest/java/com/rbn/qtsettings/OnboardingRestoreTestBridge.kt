package com.rbn.qtsettings

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingViewModel
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** Runs in the shrunk target: saved names must remain readable across an APK update. */
fun verifyMinifiedOnboardingRestore() {
    val settings = object : AppSettings {
        override val helpShown = MutableStateFlow(false)
        override val onboardingCompleted = MutableStateFlow(false)
        override fun setHelpShown(shown: Boolean) { helpShown.value = shown }
        override fun setOnboardingCompleted(completed: Boolean) { onboardingCompleted.value = completed }
    }
    val pages = listOf("Welcome", "Tiles", "Permission", "Grant", "Done")
    val methods = listOf("Adb", "Root", "Shizuku")
    for (page in pages) for (method in methods) {
        // Literal historical values detect enum-name changes as well as R8 regressions.
        val handle = SavedStateHandle(mapOf(
            "onboarding_step" to page,
            "onboarding_method" to method,
            "onboarding_tiles" to arrayListOf("Dns", "Usb")
        ))
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(handle.savedStateProvider().saveState())
            parcel.setDataPosition(0)
            val restoredHandle = SavedStateHandle.createHandle(parcel.readBundle(), null)
            val restored = OnboardingViewModel(settings, restoredHandle)
            check(restored.step.value == OnboardingStep.valueOf(page))
            check(restored.method.value == PermissionMethod.valueOf(method))
            check(restored.tilesInPanel.value == setOf(OnboardingTile.Dns, OnboardingTile.Usb))
            check(!settings.onboardingCompleted.value && !settings.helpShown.value)
        } finally {
            parcel.recycle()
        }
    }
    val unknown = OnboardingViewModel(settings, SavedStateHandle(mapOf(
        "onboarding_step" to "RetiredPage",
        "onboarding_method" to "RetiredMethod",
        "onboarding_tiles" to arrayListOf("Dns", "RetiredTile")
    )))
    check(unknown.step.value == OnboardingStep.Welcome)
    check(unknown.method.value == null)
    check(unknown.tilesInPanel.value == setOf(OnboardingTile.Dns))
}
