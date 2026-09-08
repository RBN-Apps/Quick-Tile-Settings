package com.rbn.qtsettings.app

import android.content.Context
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.viewmodel.ViewModelFactory

/** Application-scoped composition root. Never retain an Activity here.
 * Legacy preferences/factory are adapters until the feature extractions (#113–#121).
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    val preferences: PreferencesManager by lazy { PreferencesManager.getInstance(applicationContext) }
    val mainViewModelFactory: ViewModelFactory by lazy { ViewModelFactory(preferences) }
}
