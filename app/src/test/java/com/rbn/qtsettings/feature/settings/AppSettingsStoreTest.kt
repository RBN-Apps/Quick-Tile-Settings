package com.rbn.qtsettings.feature.settings

import android.content.Context
import androidx.core.content.edit
import com.rbn.qtsettings.feature.settings.data.AppSettingsStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppSettingsStoreTest {
    private val preferences get() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("app-settings-test", Context.MODE_PRIVATE)

    @Before fun clear() { preferences.edit(commit = true) { clear() } }

    @Test fun freshInstallation_startsTheFirstRun() {
        assertFalse(AppSettingsStore(preferences).onboardingCompleted.value)
    }

    @Test fun installationWithExistingSettings_keepsItsUsualEntryPoint() {
        preferences.edit(commit = true) { putBoolean("dns_toggle_off", true) }
        assertTrue(AppSettingsStore(preferences).onboardingCompleted.value)
    }

    /** The answer is written once, so defaults persisted later cannot change it. */
    @Test fun pendingFirstRun_survivesSettingsWrittenAfterwards() {
        assertFalse(AppSettingsStore(preferences).onboardingCompleted.value)
        preferences.edit(commit = true) { putBoolean("dns_toggle_off", true) }
        assertFalse(AppSettingsStore(preferences).onboardingCompleted.value)
    }

    @Test fun completionAndHelpFlag_persistIndependently() {
        val store = AppSettingsStore(preferences)
        store.setOnboardingCompleted(true)
        store.setHelpShown(true)
        val reopened = AppSettingsStore(preferences)
        assertTrue(reopened.onboardingCompleted.value)
        assertTrue(reopened.helpShown.value)
        reopened.setOnboardingCompleted(false)
        assertFalse(AppSettingsStore(preferences).onboardingCompleted.value)
        assertTrue(AppSettingsStore(preferences).helpShown.value)
    }
}
