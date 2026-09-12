package com.rbn.qtsettings.feature.automation.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** Saved dialog destination only; committed settings and policy stay in AutomationViewModel. */
class AutomationDialogViewModel(private val saved: SavedStateHandle) : ViewModel() {
    val dialog = saved.getStateFlow("automation.dialog", "")

    fun open(destination: String) { saved["automation.dialog"] = destination }
    fun dismiss() = open("")

}
