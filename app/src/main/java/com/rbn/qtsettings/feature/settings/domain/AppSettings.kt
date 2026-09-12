package com.rbn.qtsettings.feature.settings.domain

import kotlinx.coroutines.flow.StateFlow

interface AppSettings {
    val helpShown: StateFlow<Boolean>
    fun setHelpShown(shown: Boolean)
}
