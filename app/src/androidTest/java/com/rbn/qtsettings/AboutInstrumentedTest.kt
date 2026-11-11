package com.rbn.qtsettings

import android.content.ClipboardManager
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withJson
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.navigation.AppArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@AdaptiveLayout
class AboutInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)
    private fun node(id: Int) = compose.onNodeWithText(label(id))

    @Before fun prepare() { compose.dismissInitialDialogsIfPresent(context) }
    private fun open() {
        compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.About)
        node(R.string.about_description).assertIsDisplayed()
    }
    private fun scrollTo(id: Int) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label(id)))
    }
    private fun back() = compose.onNodeWithContentDescription(label(R.string.navigate_back_desc)).performClick()

    @Test fun thirdPartyLicensesAreBundled_andReturnToAboutAfterRecreation() {
        val libraries = Libs.Builder().withJson(context, R.raw.aboutlibraries).build()
        val ids = libraries.libraries.map { it.uniqueId }
        assertTrue(ids.contains("dev.rikka.shizuku:api"))
        assertTrue(ids.contains("com.google.code.gson:gson"))
        assertTrue(ids.any { it.startsWith("androidx.compose.ui:") })
        assertTrue(ids.any { it.startsWith("org.jetbrains.kotlin:kotlin-stdlib") })
        assertTrue(ids.any { it.startsWith("org.jetbrains.kotlinx:kotlinx-coroutines") })
        libraries.libraries.forEach { library ->
            assertTrue("Missing license for ${library.uniqueId}", library.licenses.isNotEmpty())
            library.licenses.forEach { license ->
                assertTrue("Missing bundled text for ${library.uniqueId}: ${license.name}",
                    !license.licenseContent.isNullOrBlank())
            }
        }

        assertTrue(libraries.libraries.first { it.uniqueId == "dev.rikka.shizuku:api" }
            .licenses.any { it.licenseContent?.contains("Copyright (c) 2021 RikkaW") == true })

        open()
        scrollTo(R.string.about_third_party_licenses)
        node(R.string.about_third_party_licenses).performClick()
        val firstLibrary = libraries.libraries.first().name
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(firstLibrary).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(firstLibrary).assertIsDisplayed().performClick()
        compose.onNodeWithText("TERMS AND CONDITIONS", substring = true).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.dialog_close)).performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(firstLibrary).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(firstLibrary).assertIsDisplayed()
        back()
        node(R.string.about_third_party_licenses).assertIsDisplayed()
    }

    @Test fun reportMetadataIsOptIn_andNewDraftResetsConsent() {
        open()
        scrollTo(R.string.about_report_issue)
        node(R.string.about_report_issue).performClick()
        val matcher = hasText(label(R.string.about_report_include)) and isToggleable()
        val checkbox = compose.onNode(matcher)
        checkbox.assertIsOff().performClick().assertIsOn()
        compose.activityRule.scenario.recreate()
        // The restored dialog attaches its own window after the Activity's first frame.
        compose.waitUntil(5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        checkbox.assertIsOn()
        node(R.string.dialog_cancel).performClick()
        node(R.string.about_report_issue).performClick()
        checkbox.assertIsOff()
        node(R.string.dialog_cancel).performClick()
        scrollTo(R.string.about_feature_request)
        node(R.string.about_feature_request).assertIsDisplayed().assertHasClickAction()
    }

    @Test fun technicalDetailsComeFromInstalledPackage_andCopyContainsOnlyDisplayedMetadata() {
        open()
        scrollTo(R.string.about_copy_technical)
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        compose.onNodeWithText("${info.versionName} (${info.longVersionCode})").assertExists()
        compose.onNodeWithText(context.packageName).assertExists()
        node(R.string.about_copy_technical).performClick()
        compose.runOnIdle {
            val copied = context.getSystemService(ClipboardManager::class.java).primaryClip!!
                .getItemAt(0).text.toString()
            assertEquals(5, copied.lines().size)
            assertTrue(copied.contains(context.packageName))
            assertTrue(copied.contains("${info.versionName} (${info.longVersionCode})"))
            assertTrue(copied.contains("API ${android.os.Build.VERSION.SDK_INT}"))
        }
    }

    @Test fun featureLinksReturnToAbout_withScrollPosition_andPermissionHelpKeepsCaller() {
        open()
        scrollTo(R.string.shortcut_settings_title)
        node(R.string.shortcut_settings_title).performClick()
        node(R.string.shortcut_settings_allow_pinned_when_disabled).assertIsDisplayed()
        back()
        node(R.string.about_shortcuts_details).assertIsDisplayed()
        scrollTo(R.string.settings_backup_title)
        node(R.string.settings_backup_title).performClick()
        node(R.string.backup_export_button).assertIsDisplayed()
        back()
        scrollTo(R.string.about_permission_setup)
        node(R.string.about_permission_setup).performClick()
        // Setup returns to its actual caller and keeps the About scroll position.
        androidx.test.espresso.Espresso.pressBack()
        node(R.string.about_permission_setup).assertIsDisplayed()
    }

    @Test fun usbCallerAndAboutScrollSurviveRecreation_andBothPredictiveEdgesCancelOrPopOnce() {
        compose.selectNavigationArea(context, AppArea.Usb)
        open()
        scrollTo(R.string.about_license)
        fun offset() = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        // Content scroll survives recreation; absolute coordinates can change with system insets.
        val before = offset()
        compose.activityRule.scenario.recreate()
        // The restored screen reaches its saved scroll position a few frames after RESUMED.
        compose.waitUntil(5_000) { runCatching { node(R.string.about_license).assertIsDisplayed() }.isSuccess }
        val restored = node(R.string.about_license).fetchSemanticsNode().positionInRoot
        assertEquals(before, offset(), 1f)
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            compose.runOnIdle {
                compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, edge))
                compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, .6f, edge))
            }
            assertTrue(node(R.string.about_license).fetchSemanticsNode().positionInRoot.x > restored.x)
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            node(R.string.about_license).assertIsDisplayed()
            assertEquals(before, offset(), 1f)
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        node(R.string.usb_menu_tile).assertIsDisplayed()
    }
}
