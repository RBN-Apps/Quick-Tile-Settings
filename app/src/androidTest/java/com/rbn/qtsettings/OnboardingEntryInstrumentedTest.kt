package com.rbn.qtsettings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.app.navigation.AppRailTarget
import com.rbn.qtsettings.platform.permissions.PermissionUtils
import com.rbn.qtsettings.platform.permissions.adbPermissionGrantCommand
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The first run in the real activity: it replaces the shell and hands back to it when finished. */
@AdaptiveLayout
@RunWith(AndroidJUnit4::class)
class OnboardingEntryInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var context: Context
    private val settings get() = AppStores.getInstance(context).app
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun setUp() {
        context = instrumentation.targetContext
        instrumentation.setInTouchMode(true)
        compose.skipOnboardingIfPresent()
    }

    /** Other classes in the run share this process-wide store and expect the shell. */
    @After fun leaveTheShellReachable() {
        compose.runOnUiThread { settings.setOnboardingCompleted(true) }
    }

    private fun text(id: Int) = context.getString(id)

    /** The indicator names the page the pager rests on, so it is the signal a move has finished. */
    private fun awaitPage(position: Int, count: Int) {
        val label = context.getString(R.string.onboarding_progress, position, count)
        // Page turns animate at a few frames per second on CI emulators; one second is too short.
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }
    private fun openFirstRun() {
        compose.runOnUiThread { settings.setOnboardingCompleted(false) }
        compose.waitForIdle()
    }

    @Test fun firstRun_replacesTheShellUntilTheLastPageIsConfirmed() {
        openFirstRun()
        compose.onNodeWithTag("onboarding").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.onboarding_welcome_subtitle)).assertIsDisplayed()
        val missing = !PermissionUtils.hasWriteSecureSettingsPermission(context)
        val count = if (missing) 5 else 3
        compose.onNodeWithTag("onboarding_start").performClick()
        awaitPage(2, count)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
        compose.onNodeWithTag("onboarding_primary").performClick()
        awaitPage(3, count)

        if (missing) {
            compose.onNodeWithText(text(R.string.onboarding_permission_title)).assertIsDisplayed()
            // By its summary: the pager prefetches the setup page, whose title repeats the row's.
            compose.onNodeWithText(text(R.string.onboarding_method_adb)).performScrollTo().performClick()
            awaitPage(4, count)
            compose.onNodeWithText(adbPermissionGrantCommand(context.packageName))
                .performScrollTo().assertIsDisplayed()
            // Back returns to the method list rather than leaving the flow.
            compose.onNodeWithContentDescription(text(R.string.navigate_back_desc)).performClick()
            awaitPage(3, count)
            compose.onNodeWithText(text(R.string.onboarding_permission_title)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.onboarding_later)).performClick()
            awaitPage(5, count)
            compose.onNodeWithText(text(R.string.onboarding_pending_title)).assertIsDisplayed()
        } else {
            compose.onNodeWithText(text(R.string.onboarding_done_title)).assertIsDisplayed()
        }
        compose.onNodeWithTag("onboarding_primary").performClick()
        compose.waitUntil { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isEmpty() }
        compose.assertNavigationAvailable(context)
    }

    @Test fun skippingOnTheFirstPage_handsOverToTheShellImmediately() {
        openFirstRun()
        compose.onNodeWithTag("onboarding_skip").performClick()
        compose.waitUntil { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isEmpty() }
        // The shell now names its selected area; the app name belongs to the welcome page.
        compose.assertNavigationAvailable(context)
        compose.onNodeWithText(text(R.string.dns_menu_providers)).assertIsDisplayed()
    }

    @Test fun settingsEntry_opensTheFlowAgainAfterItWasFinished() {
        compose.selectNavigationTarget(context, AppRailTarget.Settings)
        compose.onNodeWithText(text(R.string.onboarding_replay_title)).performScrollTo().performClick()
        compose.waitUntil { compose.onAllNodesWithTag("onboarding").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.onboarding_welcome_subtitle)).assertIsDisplayed()
    }

    @Test fun swipingForward_reachesTheNextPageWithoutTheButton() {
        openFirstRun()
        val count = if (PermissionUtils.hasWriteSecureSettingsPermission(context)) 3 else 5
        compose.onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
        awaitPage(2, count)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
    }
}
