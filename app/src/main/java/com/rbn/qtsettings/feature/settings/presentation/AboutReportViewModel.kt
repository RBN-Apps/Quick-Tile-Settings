package com.rbn.qtsettings.feature.settings.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** Consent belongs to one draft; opening another report always starts without attached metadata. */
class AboutReportViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val details = savedState.getStateFlow<String?>("reportDetails", null)
    val includeDetails = savedState.getStateFlow("includeReportDetails", false)
    fun open(details: String) {
        savedState["includeReportDetails"] = false
        savedState["reportDetails"] = details
    }
    fun include(enabled: Boolean) { savedState["includeReportDetails"] = enabled }
    fun dismiss() { savedState["reportDetails"] = null }
}
