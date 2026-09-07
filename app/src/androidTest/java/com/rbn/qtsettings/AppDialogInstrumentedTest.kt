package com.rbn.qtsettings

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.AppConfirmationDialog
import com.rbn.qtsettings.core.designsystem.AppDialog
import com.rbn.qtsettings.core.designsystem.AppTextField
import com.rbn.qtsettings.core.designsystem.ConfirmationDialogPreview
import com.rbn.qtsettings.core.designsystem.DeleteDialogPreview
import com.rbn.qtsettings.core.designsystem.FormDialogPreview
import com.rbn.qtsettings.core.designsystem.InfoDialogPreview
import com.rbn.qtsettings.core.designsystem.LongDialogPreview
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.ui.composables.dns.ConfirmDeleteDialog
import com.rbn.qtsettings.ui.composables.shortcuts.ShortcutPinnedInfoDialog
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppDialogInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun label(id: Int) = instrumentation.targetContext.getString(id)

    private inner class DialogRobot {
        var dismissals = 0
        var confirmations = 0

        fun deleteDialog() = apply {
            compose.setContent {
                QuickTileSettingsTheme {
                    ConfirmDeleteDialog(
                        DnsHostnameEntry(name = "Home DNS", hostname = "dns.example.com"),
                        onDismiss = { dismissals++ },
                        onConfirm = { confirmations++ }
                    )
                }
            }
        }

        fun click(id: Int) = apply { compose.onNodeWithText(label(id)).performClick() }

        fun assertCounts(confirmed: Int, dismissed: Int) = apply {
            compose.runOnIdle {
                assertEquals(confirmed, confirmations)
                assertEquals(dismissed, dismissals)
            }
        }
    }

    @Test fun delete_cancelDoesNotConfirm() {
        DialogRobot().deleteDialog().click(R.string.dialog_cancel).assertCounts(0, 1)
    }

    @Test fun delete_confirmAndDismissEachRunOnce() {
        DialogRobot().deleteDialog().click(R.string.button_delete).assertCounts(1, 1)
    }

    @Test fun delete_systemBackOnlyDismisses() {
        val robot = DialogRobot().deleteDialog()
        compose.waitForIdle()
        shell("input keyevent KEYCODE_BACK")
        compose.waitUntil(5_000) { robot.dismissals == 1 }
        robot.assertCounts(0, 1)
    }

    @Test fun delete_outsideTouchOnlyDismisses() {
        val robot = DialogRobot().deleteDialog()
        // Left of the platform dialog window, inside the activity's content area.
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input tap 1 400")
        ).use { it.readBytes() }
        compose.waitUntil { robot.dismissals == 1 }
        robot.assertCounts(0, 1)
    }

    @Test fun disabledConfirmationCannotExecute_butCancelRemainsAvailable() {
        val robot = DialogRobot()
        compose.setContent {
            QuickTileSettingsTheme {
                AppConfirmationDialog(
                    title = label(R.string.confirm_delete_hostname_title),
                    message = "Home DNS",
                    confirmLabel = label(R.string.button_delete),
                    onConfirm = { robot.confirmations++ },
                    onDismissRequest = { robot.dismissals++ },
                    confirmEnabled = false,
                    destructive = true
                )
            }
        }
        compose.onNodeWithText(label(R.string.button_delete)).assertIsNotEnabled().performTouchInput { click() }
        robot.click(R.string.dialog_cancel).assertCounts(0, 1)
    }

    @Test fun info_exposesDialogHeadingAndCloseAction() {
        var dismissed = 0
        compose.setContent {
            QuickTileSettingsTheme { ShortcutPinnedInfoDialog { dismissed++ } }
        }
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText(label(R.string.shortcut_settings_allow_pinned_when_disabled))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val closeLabel = label(R.string.dialog_close)
        shell("input keyevent KEYCODE_TAB")
        compose.onNodeWithText(closeLabel).assertIsFocused()
        shell("input keyevent KEYCODE_ENTER")
        compose.waitUntil(5_000) { dismissed == 1 }
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test fun longFormScrollsToErrorAndField_actionsRemainVisible_cancelDoesNotSave() {
        var draft by mutableStateOf("invalid")
        var saved: String? = null
        lateinit var dialogView: View
        var dismissed = 0
        compose.setContent {
            QuickTileSettingsTheme {
                AppDialog(
                    title = label(R.string.dns_add_hostname_title),
                    onDismissRequest = { dismissed++ },
                    confirmLabel = label(R.string.dialog_save),
                    onConfirm = { saved = draft },
                    dismissLabel = label(R.string.dialog_cancel)
                ) {
                    dialogView = LocalView.current
                    Text(label(R.string.network_type_info_message))
                    Spacer(Modifier.height(16.dp))
                    AppTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = label(R.string.dns_hostname_value_label),
                        errorText = label(R.string.error_hostname_value_invalid)
                    )
                }
            }
        }
        compose.onNodeWithText(label(R.string.dialog_save)).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performScrollTo().performTouchInput { click() }
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, label(R.string.error_hostname_value_invalid))
        )
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(dialogView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("dns.example.com")
        compose.onNode(hasSetTextAction()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(label(R.string.dialog_save)).assertIsDisplayed()
        // assertIsDisplayed alone cannot detect Android panning a dialog behind the IME.
        val visibleFrame = Rect()
        val location = IntArray(2)
        compose.runOnIdle {
            dialogView.getWindowVisibleDisplayFrame(visibleFrame)
            dialogView.getLocationOnScreen(location)
        }
        for (node in listOf(compose.onNode(hasSetTextAction()),
            compose.onNodeWithText(label(R.string.dialog_save)),
            compose.onNodeWithText(label(R.string.dialog_cancel)))) {
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Control must stay above the IME", location[1] + bounds.bottom <= visibleFrame.bottom)
            assertTrue("Control must stay below the status bar", location[1] + bounds.top >= visibleFrame.top)
        }
        screenshot("form-ime", fullScreen = true)
        compose.onNodeWithText(label(R.string.dialog_cancel)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("dns.example.com", draft)
            assertEquals(null, saved)
            assertEquals(1, dismissed)
        }
    }

    @Test fun infoLightEnglish() = gallery("info-light-en", "en", false) { InfoDialogPreview() }
    @Test fun infoDarkGerman() = gallery("info-dark-de", "de", true) { InfoDialogPreview() }
    @Test fun confirmationEnglish() = gallery("confirmation-en", "en", false) { ConfirmationDialogPreview() }
    @Test fun destructiveGerman() = gallery("delete-de", "de", false) { DeleteDialogPreview() }
    @Test fun formErrorDarkGerman() = gallery("form-dark-de", "de", true) { FormDialogPreview() }
    @Test fun longContentLargeGerman() {
        // Dialogs create a new Android composition root. Change the real configuration;
        // a LocalDensity override outside that root does not exercise system font scaling.
        val previousScale = shell("settings get system font_scale").trim()
        try {
            shell("settings put system font_scale 2.0")
            compose.waitUntil(10_000) {
                instrumentation.targetContext.resources.configuration.fontScale == 2f
            }
            gallery("long-large-de", "de", false) { LongDialogPreview() }
            val scrollNode = compose.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
            )
            scrollNode.assert(SemanticsMatcher("content exceeds viewport") {
                it.config[SemanticsProperties.VerticalScrollAxisRange].maxValue() > 0f
            })
            scrollNode.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
                it(0f, 10000f)
            }
            compose.onNodeWithText("Schließen").assertIsDisplayed()
            screenshot("long-large-de-scrolled")
        } finally {
            if (previousScale == "null") shell("settings delete system font_scale")
            else shell("settings put system font_scale $previousScale")
            val restoredScale = previousScale.toFloatOrNull() ?: 1f
            compose.waitUntil(10_000) {
                instrumentation.targetContext.resources.configuration.fontScale == restoredScale
            }
        }
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }

    private fun gallery(
        name: String, locale: String, dark: Boolean,
        content: @Composable () -> Unit
    ) {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                setLocale(Locale.forLanguageTag(locale))
                uiMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val localizedContext = LocalContext.current.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration
            ) { content() }
        }
        compose.onNode(isDialog()).assertExists()
        screenshot(name)
    }

    private fun screenshot(name: String, fullScreen: Boolean = false) {
        if (InstrumentationRegistry.getArguments().getString("dialogScreenshots") != "true") return
        compose.waitForIdle()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use {
            val bitmap = if (fullScreen) instrumentation.uiAutomation.takeScreenshot()
                else compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "cp ${file.absolutePath} /sdcard/Download/qt-dialog-$name.png"
        )).use { it.readBytes() }
    }
}
