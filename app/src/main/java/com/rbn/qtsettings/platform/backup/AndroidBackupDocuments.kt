package com.rbn.qtsettings.platform.backup

import android.content.Context
import androidx.core.net.toUri
import com.rbn.qtsettings.feature.settings.domain.BackupDocuments
import com.rbn.qtsettings.feature.settings.domain.SettingsBackupAccess

class AndroidBackupDocuments(context: Context, private val backup: SettingsBackupAccess) : BackupDocuments {
    private val resolver = context.applicationContext.contentResolver
    override fun export(destination: String) {
        val json = backup.exportSettingsBackupJson()
        resolver.openOutputStream(destination.toUri())?.use { stream ->
            stream.writer(Charsets.UTF_8).use { it.write(json) }
        } ?: error("Could not open backup destination")
    }
    override fun restore(source: String) {
        val json = resolver.openInputStream(source.toUri())?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } ?: error("Could not open backup file")
        backup.restoreSettingsBackupJson(json)
    }
}
