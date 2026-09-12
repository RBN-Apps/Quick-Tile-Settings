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
    private companion object { const val KEY_HELP_SHOWN = "help_shown_v1" }
}
