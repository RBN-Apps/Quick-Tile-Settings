package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Page state for the first run. Permission truth and grants stay with the permission owner, and
 * which page the user can reach by swiping is the pager's business. This model records the page
 * that is open so it survives process death, and opens one when the flow itself decides to.
 */
class OnboardingViewModel(
    private val settings: AppSettings,
    private val handle: SavedStateHandle
) : ViewModel() {
    val completed = settings.onboardingCompleted

    private val current = MutableStateFlow(handle.read(KEY_STEP, OnboardingStep.entries) ?: OnboardingStep.Welcome)
    val step = current.asStateFlow()

    private val chosen = MutableStateFlow(handle.read(KEY_METHOD, PermissionMethod.entries))
    val method = chosen.asStateFlow()

    private val placed = MutableStateFlow(handle.readAll(KEY_TILES, OnboardingTile.entries))
    val tilesInPanel = placed.asStateFlow()

    /** What the system reported after its add-tile dialog, including a tile that was already there. */
    fun onTileResult(tile: OnboardingTile, present: Boolean) {
        val updated = if (present) placed.value + tile else placed.value - tile
        if (updated == placed.value) return
        placed.value = updated
        handle[KEY_TILES] = ArrayList(updated.map { it.name })
    }

    /** The page the user arrived at, whether by swiping or by an action. */
    fun show(step: OnboardingStep) {
        if (current.value == step) return
        current.value = step
        handle[KEY_STEP] = step.name
    }

    fun choose(method: PermissionMethod) {
        chosen.value = method
        handle[KEY_METHOD] = method.name
        show(OnboardingStep.Grant)
    }

    /** Both setup pages end here when the user would rather grant the permission another day. */
    fun skipSetup() = show(OnboardingStep.Done)

    /** The setup page is finished the moment Android reports the permission. */
    fun onPermissionGranted() {
        if (current.value == OnboardingStep.Grant || current.value == OnboardingStep.Permission)
            show(OnboardingStep.Done)
    }

    fun finish() {
        // The one-time permission page has been replaced by this flow for the rest of the install.
        settings.setHelpShown(true)
        settings.setOnboardingCompleted(true)
    }

    /** Re-entry from settings; the flow starts over at its first page. */
    fun restart() {
        chosen.value = null
        handle[KEY_METHOD] = null
        placed.value = emptySet()
        handle[KEY_TILES] = null
        show(OnboardingStep.Welcome)
        settings.setOnboardingCompleted(false)
    }

    private companion object {
        const val KEY_STEP = "onboarding_step"
        const val KEY_METHOD = "onboarding_method"
        const val KEY_TILES = "onboarding_tiles"
        /** Saved names survive R8 and an unknown one falls back instead of crashing on restore. */
        fun <T : Enum<T>> SavedStateHandle.read(key: String, values: List<T>): T? =
            get<String>(key)?.let { name -> values.firstOrNull { it.name == name } }
        fun <T : Enum<T>> SavedStateHandle.readAll(key: String, values: List<T>): Set<T> =
            get<ArrayList<String>>(key).orEmpty()
                .mapNotNull { name -> values.firstOrNull { it.name == name } }.toSet()
    }
}
