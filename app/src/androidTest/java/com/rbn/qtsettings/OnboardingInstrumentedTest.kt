package com.rbn.qtsettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingTile
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingScreen
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingUiActions
import com.rbn.qtsettings.feature.onboarding.presentation.OnboardingUiState
import com.rbn.qtsettings.feature.permissions.domain.PermissionMethod
import com.rbn.qtsettings.feature.permissions.domain.PermissionSnapshot
import com.rbn.qtsettings.feature.permissions.presentation.PermissionOperation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Page rendering, paging by button and swipe, and the intents each control reports. */
@RunWith(AndroidJUnit4::class)
class OnboardingInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recorded = mutableListOf<String>()
    private val actions = object : OnboardingUiActions {
        override fun show(step: OnboardingStep) { recorded += "show:${step.name}" }
        override fun skip() { recorded += "skip" }
        override fun finish() { recorded += "finish" }
        override fun later() { recorded += "later" }
        override fun chooseMethod(method: PermissionMethod) { recorded += "choose:${method.name}" }
        override fun addTile(tile: OnboardingTile) { recorded += "addTile:${tile.name}" }
        override fun refresh() { recorded += "refresh" }
        override fun copyCommand(command: String) { recorded += "copy:$command" }
        override fun openDeveloperOptions() { recorded += "developerOptions" }
        override fun requestShizukuPermission() { recorded += "requestShizuku" }
        override fun grantWithShizuku() { recorded += "grantShizuku" }
        override fun grantWithRoot() { recorded += "grantRoot" }
    }
    private val command = "adb shell pm grant com.rbn.qtsettings android.permission.WRITE_SECURE_SETTINGS"

    private fun show(
        step: OnboardingStep,
        snapshot: PermissionSnapshot = PermissionSnapshot(),
        method: PermissionMethod? = null,
        operation: PermissionOperation = PermissionOperation(),
        tilesInPanel: Set<OnboardingTile> = emptySet(),
        canAddTiles: Boolean = true
    ) {
        compose.setContent {
            QuickTileSettingsTheme {
                OnboardingScreen(OnboardingUiState(step, snapshot, true, operation, method, command,
                    tilesInPanel = tilesInPanel, canAddTiles = canAddTiles, decorativeMotion = false), actions)
            }
        }
        // The pager reports the page it opens on; assertions care about what follows.
        compose.waitForIdle()
        recorded.clear()
    }

    private fun text(id: Int) = context.getString(id)
    private fun pager() = compose.onNodeWithTag("onboarding_pager")

    /** The indicator names the page the pager rests on, so it is the signal a move has finished. */
    private fun awaitPage(position: Int, count: Int) {
        val label = context.getString(R.string.onboarding_progress, position, count)
        compose.waitUntil {
            compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    @Test fun welcomePage_startsTheFlowOrLeavesItWithoutABackAffordance() {
        show(OnboardingStep.Welcome)
        compose.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.onboarding_welcome_subtitle)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.navigate_back_desc)).assertDoesNotExist()
        compose.onNodeWithTag("onboarding_skip").performClick()
        assertEquals(listOf("skip"), recorded)
    }

    @Test fun forwardButtonAndBackArrow_moveOnePageEachAndReportIt() {
        show(OnboardingStep.Welcome)
        compose.onNodeWithTag("onboarding_start").performClick()
        awaitPage(2, 5)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.navigate_back_desc)).performClick()
        awaitPage(1, 5)
        compose.onNodeWithText(text(R.string.onboarding_welcome_subtitle)).assertIsDisplayed()
        assertEquals(listOf("show:Tiles", "show:Welcome"), recorded)
    }

    @Test fun swipingHorizontally_movesBetweenPagesInBothDirections() {
        show(OnboardingStep.Tiles)
        pager().performTouchInput { swipeLeft() }
        awaitPage(3, 5)
        compose.onNodeWithText(text(R.string.onboarding_permission_title)).assertIsDisplayed()
        pager().performTouchInput { swipeRight() }
        awaitPage(2, 5)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
        assertEquals(listOf("show:Permission", "show:Tiles"), recorded)
    }

    @Test fun tilesPage_namesWhatTheTwoTilesDo() {
        show(OnboardingStep.Tiles)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
        listOf(R.string.onboarding_highlight_dns, R.string.onboarding_highlight_usb,
            R.string.onboarding_highlight_revert, R.string.onboarding_highlight_shortcuts)
            .forEach { compose.onNodeWithText(text(it)).performScrollTo().assertIsDisplayed() }
    }

    @Test fun permissionPage_reportsTheChosenMethodAndCanBeDeferred() {
        show(OnboardingStep.Permission, PermissionSnapshot(shizukuAvailable = true))
        compose.onNodeWithText(text(R.string.onboarding_permission_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.permission_not_setup)).assertIsDisplayed()
        // Shizuku is present, so the page recommends it ahead of the other two.
        compose.onNodeWithText(text(R.string.permission_recommended)).performScrollTo().assertIsDisplayed()
        // By its summary: the pager prefetches the setup page, whose title repeats the row's.
        compose.onNodeWithText(text(R.string.onboarding_method_shizuku_ready)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.onboarding_later)).performClick()
        assertEquals(listOf("choose:Shizuku", "later"), recorded)
    }

    /**
     * The permission normally arrives from a computer while the setup page is open. That drops two
     * pages from the flow underneath a pager that still holds them, which used to take the app down
     * in the draw pass.
     */
    @Test fun permissionArrivingOnTheSetupPage_shortensTheFlowToItsLastPage() {
        val granted = androidx.compose.runtime.mutableStateOf(false)
        val step = androidx.compose.runtime.mutableStateOf(OnboardingStep.Permission)
        compose.setContent {
            QuickTileSettingsTheme {
                OnboardingScreen(OnboardingUiState(step.value,
                    PermissionSnapshot(hasWriteSecureSettings = granted.value), true,
                    PermissionOperation(), PermissionMethod.Adb, command,
                    decorativeMotion = false), actions)
            }
        }
        awaitPage(3, 5)
        // Choosing a method turns the pager onto the setup page, which composes the page behind it
        // off screen. That page is the one the shorter flow no longer has.
        compose.runOnIdle { step.value = OnboardingStep.Grant }
        awaitPage(4, 5)
        compose.runOnIdle { granted.value = true }
        awaitPage(3, 3)
        compose.onNodeWithText(text(R.string.onboarding_done_title)).assertIsDisplayed()
    }

    @Test fun adbSetupPage_showsTheGrantCommandAndCopiesIt() {
        show(OnboardingStep.Grant, method = PermissionMethod.Adb)
        compose.onNodeWithText(text(R.string.permission_adb_intro)).assertIsDisplayed()
        compose.onNodeWithText(command).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.button_copy_grant_command)).performScrollTo().performClick()
        assertEquals(listOf("copy:$command"), recorded)
    }

    /** Swiping past the method list instead of picking one still opens a usable way. */
    @Test fun setupPageWithoutAChosenMethod_showsTheRecommendedOne() {
        show(OnboardingStep.Grant, PermissionSnapshot(rooted = true))
        compose.onNodeWithText(text(R.string.onboarding_grant_root_subtitle)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.permission_root_detected)).performScrollTo().assertIsDisplayed()
    }

    @Test fun runningCheck_blocksTheCheckButtonWhileItIsInFlight() {
        show(OnboardingStep.Grant, method = PermissionMethod.Root,
            operation = PermissionOperation(checking = true))
        compose.onNodeWithText(text(R.string.permission_checking)).assertIsDisplayed()
        compose.onNodeWithTag("onboarding_primary").assertIsNotEnabled()
    }

    /** A granted permission drops both setup pages, so the flow is three pages long. */
    @Test fun grantedPermission_endsOnTheReadyPageWithoutTheSetupPages() {
        show(OnboardingStep.Done, PermissionSnapshot(hasWriteSecureSettings = true))
        compose.onNodeWithText(text(R.string.onboarding_done_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.onboarding_add_tile_title)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.navigate_back_desc)).performClick()
        awaitPage(2, 3)
        compose.onNodeWithText(text(R.string.onboarding_tiles_title)).assertIsDisplayed()
        compose.onNodeWithTag("onboarding_primary").performClick()
        awaitPage(3, 3)
        compose.onNodeWithText(text(R.string.onboarding_done_title)).assertIsDisplayed()
        assertEquals(listOf("show:Tiles", "show:Done"), recorded)
    }

    /** Android 13 places the tile itself; the row asks for it and reports what came back. */
    @Test fun readyPage_offersBothTilesAndMarksTheOneAlreadyInThePanel() {
        show(OnboardingStep.Done, PermissionSnapshot(hasWriteSecureSettings = true),
            tilesInPanel = setOf(OnboardingTile.Dns))
        compose.onNodeWithText(text(R.string.onboarding_tile_added)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.onboarding_tile_add_summary)).performScrollTo().performClick()
        assertEquals(listOf("addTile:Usb"), recorded)
    }

    /** Below Android 13 nothing can be placed for the user, so the page explains the panel instead. */
    @Test fun withoutTheSystemRequest_theReadyPageKeepsTheWrittenInstructions() {
        show(OnboardingStep.Done, PermissionSnapshot(hasWriteSecureSettings = true), canAddTiles = false)
        compose.onNodeWithText(text(R.string.onboarding_add_tile_summary)).performScrollTo().assertIsDisplayed()
    }

    @Test fun missingPermission_endsOnAPageThatSaysWhereToResume() {
        show(OnboardingStep.Done)
        compose.onNodeWithText(text(R.string.onboarding_pending_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.onboarding_pending_hint)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("onboarding_primary").performClick()
        assertEquals(listOf("finish"), recorded)
    }
}
