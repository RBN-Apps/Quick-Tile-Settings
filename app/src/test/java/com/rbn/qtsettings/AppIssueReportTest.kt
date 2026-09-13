package com.rbn.qtsettings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.rbn.qtsettings.feature.settings.presentation.AboutReportViewModel
import com.rbn.qtsettings.platform.app.AppInfo
import com.rbn.qtsettings.platform.app.bugReportUrl
import com.rbn.qtsettings.platform.app.featureRequestUrl
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppIssueReportTest {
    private val project = "https://github.com/RBN-Apps/Quick-Tile-Settings"
    private val info = AppInfo("1.5 & test", 12, "com.rbn.qtsettings", "15", 35,
        "Manufacturer", "Private test model", "arm64-v8a")

    @Test fun decliningMetadataDoesNotLeakDetails_andRequiredPublicFormInputsRemainOptional() {
        val uri = Uri.parse(bugReportUrl(project, info, null))
        assertEquals("bug-report.yml", uri.getQueryParameter("template"))
        assertNull(uri.getQueryParameter("additional-context"))
        for (field in listOf("app-version", "android-version", "device-model"))
            assertEquals("Not provided", uri.getQueryParameter(field))
        assertFalse(uri.toString().contains("Manufacturer"))
    }

    @Test fun selectedMetadataUsesActualFormFields_andUrlEncodingPreservesPreview() {
        val preview = "Language: de-DE\nDevice: A & B / Test #1\nFont: 1.3"
        val uri = Uri.parse(bugReportUrl(project, info, preview))
        assertEquals("1.5 & test (12)", uri.getQueryParameter("app-version"))
        assertEquals("15 / API 35", uri.getQueryParameter("android-version"))
        assertEquals("Manufacturer Private test model", uri.getQueryParameter("device-model"))
        assertEquals("### Technical information\n\n$preview", uri.getQueryParameter("additional-context"))
        assertNull(uri.fragment)
    }

    @Test fun featureRequestOpensItsOwnFormWithoutDeviceInformation() {
        val uri = Uri.parse(featureRequestUrl(project))
        assertEquals(setOf("template"), uri.queryParameterNames)
        assertEquals("feature-request.yml", uri.getQueryParameter("template"))
    }

    @Test fun consentSurvivesRecreation_butNewReportStartsUnchecked() {
        val saved = SavedStateHandle()
        val original = AboutReportViewModel(saved)
        original.open("Preview")
        assertFalse(original.includeDetails.value)
        original.include(true)
        val restored = AboutReportViewModel(saved)
        assertTrue(restored.includeDetails.value)
        assertEquals("Preview", restored.details.value)
        restored.dismiss()
        assertNull(restored.details.value)
        restored.open("New preview")
        assertFalse(restored.includeDetails.value)
    }
}
