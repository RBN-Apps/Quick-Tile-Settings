package com.rbn.qtsettings

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import com.rbn.qtsettings.app.AppStores
import org.junit.Rule
import org.junit.Test

class GlobalHelpNavigationInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun text(id: Int) = compose.onNodeWithText(context.getString(id))

    @Test fun adbPage_recreationAndBackReturnThroughSetupToCaller() {
        val store = AppStores.getInstance(context).app
        val oldHelp = store.helpShown.value
        store.setHelpShown(true)
        try {
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
                instrumentation.setInTouchMode(true)
                compose.selectNavigationTarget(context, com.rbn.qtsettings.app.navigation.AppRailTarget.Permissions)
                text(R.string.button_show_adb_instructions).performScrollTo().performClick()
                text(R.string.adb_instructions_title).assertIsDisplayed()
                text(R.string.menu_permission_help).assertDoesNotExist()
                compose.onAllNodes(isDialog()).assertCountEquals(0)
                scenario.recreate()
                text(R.string.adb_instructions_title).assertIsDisplayed()
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                text(R.string.menu_permission_help).assertIsDisplayed()
                compose.onAllNodes(isDialog()).assertCountEquals(0)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                compose.assertNavigationAvailable(context)
                compose.onAllNodes(isDialog()).assertCountEquals(0)
            }
        } finally { store.setHelpShown(oldHelp) }
    }
}
