package com.rbn.qtsettings.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.automation.presentation.AutomationViewModel
import org.junit.After
import org.junit.Before
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

abstract class SettingsIntegrationFixture {

    protected lateinit var context: Context
    protected lateinit var prefsManager: AppStores
    protected lateinit var automation: AutomationViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetAppStoresSingleton()
        clearPreferences()
        prefsManager = AppStores.getInstance(context)
        automation = AutomationViewModel(prefsManager.automation, { com.rbn.qtsettings.platform.automation.AndroidWifiDiscoverySource(it) })
        automation.setApplicationContext(context)
    }

    @After
    fun tearDown() {
        shadowOf(context.applicationContext as Application).denyPermissions(
            Manifest.permission.WRITE_SECURE_SETTINGS,
            Manifest.permission.POST_NOTIFICATIONS
        )
        clearPreferences()
        resetAppStoresSingleton()
    }

    protected fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    protected fun resetAppStoresSingleton() {
        val instanceField = com.rbn.qtsettings.app.AppStores::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

}
