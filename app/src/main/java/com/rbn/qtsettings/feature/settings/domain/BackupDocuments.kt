package com.rbn.qtsettings.feature.settings.domain

/** Opaque document references; Android URI and stream handling stay in the platform adapter. */
interface BackupDocuments {
    fun export(destination: String)
    fun restore(source: String)
}
