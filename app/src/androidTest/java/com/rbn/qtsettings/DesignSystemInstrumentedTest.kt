package com.rbn.qtsettings

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.AppTextField
import com.rbn.qtsettings.core.designsystem.DesignSystemInputsGallery
import com.rbn.qtsettings.core.designsystem.DesignSystemRowsGallery
import com.rbn.qtsettings.core.designsystem.SettingsCheckboxRow
import com.rbn.qtsettings.core.designsystem.SettingsNavigationRow
import com.rbn.qtsettings.core.designsystem.SettingsSwitchRow
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

class DesignSystemInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toggleRows_exposeOneActionAndState_andClickOnceOnLabelOrControl() {
        var switch by mutableStateOf(false)
        var checkbox by mutableStateOf(false)
        var switchCalls = 0
        var checkboxCalls = 0
        compose.setContent {
            QuickTileSettingsTheme {
                Column {
                    SettingsSwitchRow("Switch", switch, { switch = it; switchCalls++ }, Modifier.testTag("switch"))
                    SettingsCheckboxRow("Checkbox", checkbox, { checkbox = it; checkboxCalls++ }, Modifier.testTag("checkbox"))
                }
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
        for ((tag, role) in listOf("switch" to Role.Switch, "checkbox" to Role.Checkbox)) {
            val row = compose.onNodeWithTag(tag)
            row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))
                .assertHeightIsAtLeast(48.dp).performClick().assertIsOn()
            // Tap the visual control, not just the text/accessibility action.
            row.performTouchInput { click(Offset(width - 40.dp.toPx(), height / 2f)) }
        }
        compose.runOnIdle {
            assertEquals(2, switchCalls)
            assertEquals(2, checkboxCalls)
            assertEquals(false, switch)
            assertEquals(false, checkbox)
        }
    }

    @Test
    fun disabledRows_andButton_rejectTouchActions() {
        var calls = 0
        compose.setContent {
            QuickTileSettingsTheme {
                Column {
                    SettingsSwitchRow("Switch", false, { calls++ }, Modifier.testTag("switch"), enabled = false)
                    SettingsCheckboxRow("Checkbox", true, { calls++ }, Modifier.testTag("checkbox"), enabled = false)
                    SettingsNavigationRow("Navigation", Icons.Outlined.Dns, { calls++ },
                        Modifier.testTag("navigation"), enabled = false)
                    AppButton("Delete", { calls++ }, Modifier.testTag("button"),
                        style = AppButtonStyle.Destructive, enabled = false)
                }
            }
        }
        listOf("switch", "checkbox", "navigation", "button").forEach {
            compose.onNodeWithTag(it).assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
                .performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun switchRow_supportsKeyboardActivation() {
        var checked by mutableStateOf(false)
        var calls = 0
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            QuickTileSettingsTheme {
                SettingsSwitchRow("Unlock", checked, { checked = it; calls++ }, Modifier.testTag("row"))
            }
        }
        // Compose key injection bypasses Android's touch-mode switch. Enter keyboard
        // mode explicitly, as a hardware keyboard does, before requesting focus.
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithTag("row").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("row").assertIsFocused()
        compose.onNodeWithTag("row").performKeyInput { pressKey(Key.Spacebar) }.assertIsOn()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun navigationRow_exposesSingleButtonActionAndReadableStatus() {
        var calls = 0
        compose.setContent {
            QuickTileSettingsTheme {
                SettingsNavigationRow("DNS", Icons.Outlined.Dns, { calls++ },
                    Modifier.testTag("row"), summary = "Provider and tile", status = "Automatic")
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithTag("row").assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        compose.onNodeWithText("Automatic").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun textField_exposesSpecificErrorAndForwardsEditableText() {
        var value by mutableStateOf("")
        compose.setContent {
            QuickTileSettingsTheme {
                AppTextField(value, { value = it }, "Hostname", Modifier.testTag("field"),
                    supportingText = "Help", errorText = "Invalid hostname")
            }
        }
        compose.onNodeWithTag("field")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Invalid hostname"))
            .performTextInput("dns.example.com")
        compose.onNodeWithText("Invalid hostname").assertIsDisplayed()
        compose.runOnIdle { assertEquals("dns.example.com", value) }
    }

    @Test fun rowsGallery_lightEnglish() = renderGallery("rows-light-en", false, "en", 1f, false)
    @Test fun rowsGallery_darkGerman() = renderGallery("rows-dark-de", true, "de", 1f, false)
    @Test fun rowsGallery_largeGerman() = renderGallery("rows-large-de", false, "de", 2f, false)
    @Test fun inputsGallery_lightEnglish() = renderGallery("inputs-light-en", false, "en", 1f, true)
    @Test fun inputsGallery_darkGerman() = renderGallery("inputs-dark-de", true, "de", 1f, true)
    @Test fun inputsGallery_largeGerman() = renderGallery("inputs-large-de", false, "de", 2f, true)

    private fun renderGallery(name: String, dark: Boolean, language: String, fontScale: Float, inputs: Boolean) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            this.fontScale = fontScale
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)
            ) {
                QuickTileSettingsTheme(darkTheme = dark, dynamicColor = true) {
                    Surface(Modifier.width(320.dp)) {
                        if (inputs) DesignSystemInputsGallery() else DesignSystemRowsGallery()
                    }
                }
            }
        }
        compose.waitForIdle()
        captureIfRequested(name)
        // Long translated content must remain reachable, including below the initial viewport.
        val lastLabel = localizedContext.getString(
            if (inputs) R.string.button_delete else R.string.settings_backup_title
        )
        compose.onNodeWithText(lastLabel).performScrollTo().assertIsDisplayed()
        if (fontScale > 1f) captureIfRequested("$name-scrolled")
    }

    private fun captureIfRequested(name: String) {
        if (InstrumentationRegistry.getArguments().getString("designSystemScreenshots") != "true") return
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design-system")
        directory.mkdirs()
        val file = File(directory, "$name.png")
        file.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val command = "cp ${file.absolutePath} /sdcard/Download/qt-design-system-$name.png"
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
}
