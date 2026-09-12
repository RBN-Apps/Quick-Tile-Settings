package com.rbn.qtsettings.feature.settings.domain

interface SettingsBackupAccess {
    fun exportSettingsBackupJson(): String
    fun restoreSettingsBackupJson(json: String)
}
