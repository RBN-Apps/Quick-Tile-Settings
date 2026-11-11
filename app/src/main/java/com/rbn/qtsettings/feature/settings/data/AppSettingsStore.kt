package com.rbn.qtsettings.feature.settings.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.rbn.qtsettings.feature.settings.domain.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-local onboarding state; intentionally excluded from portable backups. */
class AppSettingsStore(private val preferences: SharedPreferences) : AppSettings {
    private val shown = MutableStateFlow(preferences.getBoolean(KEY_HELP_SHOWN, false))
    override val helpShown = shown.asStateFlow()
    override fun setHelpShown(shown: Boolean) {
        preferences.edit { putBoolean(KEY_HELP_SHOWN, shown) }
        this.shown.value = shown
    }

    private val completed = MutableStateFlow(readOrSeedOnboarding())
    override val onboardingCompleted = completed.asStateFlow()
    override fun setOnboardingCompleted(completed: Boolean) {
        preferences.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
        this.completed.value = completed
    }

    /**
     * An installation that already carries settings has passed the previous first run and keeps
     * its usual entry point. The answer is written on the first read, because the other stores
     * persist their defaults moments later and an empty file is the only reliable signal.
     */
    private fun readOrSeedOnboarding(): Boolean {
        if (preferences.contains(KEY_ONBOARDING_COMPLETED))
            return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val existingInstallation = preferences.all.isNotEmpty()
        preferences.edit(commit = true) { putBoolean(KEY_ONBOARDING_COMPLETED, existingInstallation) }
        return existingInstallation
    }

    private companion object {
        const val KEY_HELP_SHOWN = "help_shown_v1"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed_v1"
    }
}
