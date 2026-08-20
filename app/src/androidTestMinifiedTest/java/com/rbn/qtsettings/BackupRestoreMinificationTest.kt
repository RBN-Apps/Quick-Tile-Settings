package com.rbn.qtsettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreMinificationTest {

    @Test
    fun exportedBackup_canBeRestoredWithStableJsonFieldNames() {
        verifyMinifiedBackupRoundTrip(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
    }

    @Test
    fun legacyMinifiedBackups_canBeRestored() {
        verifyLegacyMinifiedBackupRestore(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
    }
}
