package com.rbn.qtsettings.feature.automation

import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.feature.automation.presentation.AutomationDnsTargetViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationDnsTargetViewModelTest {
    @Test fun searchRestoresWithItsNavigationEntry() {
        val saved = SavedStateHandle()
        AutomationDnsTargetViewModel(saved).search("quad9")
        val restored = AutomationDnsTargetViewModel(SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }))
        assertEquals("quad9", restored.query.value)
        restored.search("")
        assertEquals("", restored.query.value)
    }
}
