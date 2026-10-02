package com.rbn.qtsettings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@AdaptiveLayout
class ShortcutEntryInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)

    @Before fun prepare() {
        compose.dismissInitialDialogsIfPresent(context)
    }

    private fun enter() {
        compose.onNodeWithText(label(R.string.shortcut_menu_summary))
            .performScrollTo().performClick()
    }

    private fun assertGlobalCounts() {
        val config = AppStores.getInstance(context).shortcuts.state.value
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText(context.getString(R.string.shortcut_settings_limit,
            config.enabledIds.size, config.maxCount)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.shortcut_favorites_count,
            config.favoriteIds.size, ShortcutCatalog.MAX_FAVORITE_SHORTCUTS)).assertIsDisplayed()
    }

    @Test fun dnsEntry_targetsDns_andBackReturnsToOverview() {
        enter()
        compose.onNodeWithText(label(R.string.setting_title_private_dns)).assertIsDisplayed()
        assertGlobalCounts()
        compose.onNodeWithContentDescription(label(R.string.navigate_back_desc)).performClick()
        compose.onNodeWithText(label(R.string.shortcut_menu_summary)).assertIsDisplayed()
    }

    @Test fun usbEntry_keepsGlobalCounts_andRestoresUserScrollPosition() {
        compose.selectNavigationArea(context, com.rbn.qtsettings.app.navigation.AppArea.Usb)
        enter()
        val heading = compose.onNodeWithText(label(R.string.setting_title_usb_debugging))
        heading.assertIsDisplayed()
        val listTop = compose.onNode(hasScrollToIndexAction()).getUnclippedBoundsInRoot().top
        assertTrue("USB heading should align below the global overview",
            heading.getUnclippedBoundsInRoot().top - listTop < 24.dp)
        assertGlobalCounts()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText(label(R.string.shortcut_settings_allow_pinned_when_disabled)).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(label(R.string.shortcut_settings_allow_pinned_when_disabled)).assertIsDisplayed()
        assertGlobalCounts()
        compose.onNodeWithContentDescription(label(R.string.navigate_back_desc)).performClick()
        compose.onNodeWithText(label(R.string.usb_menu_tile)).performScrollTo().assertIsDisplayed()
    }

    @Test fun globalEntry_explanationOpensFromOverview_andBackKeepsTheList() {
        compose.navigateToShortcutSettings(context)
        val pinnedFallback = label(R.string.shortcut_settings_allow_pinned_when_disabled)
        compose.onNodeWithText(pinnedFallback).assertIsDisplayed()
        assertGlobalCounts()
        compose.onNodeWithText(label(R.string.shortcut_help_title)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.shortcut_help_intro)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.shortcut_help_step_pin_title)).performScrollTo().assertIsDisplayed()
        val config = AppStores.getInstance(context).shortcuts.state.value
        // The device-dependent limit and the favorite count are named in the explanation itself.
        compose.onNodeWithText(context.getString(R.string.shortcut_settings_description, config.maxCount,
            ShortcutCatalog.MAX_FAVORITE_SHORTCUTS)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(label(R.string.shortcut_help_launcher_note)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.navigate_back_desc)).performClick()
        compose.onNodeWithText(pinnedFallback).assertIsDisplayed()
        assertGlobalCounts()
    }

    @Test fun explanation_survivesRecreation_andLinksToPermissionSetup() {
        compose.navigateToShortcutSettings(context)
        compose.onNodeWithText(label(R.string.shortcut_help_title)).performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(label(R.string.shortcut_help_intro)).assertIsDisplayed()
        // The rail may show the same page title, so the row is addressed by its summary.
        compose.onNodeWithText(label(R.string.shortcut_help_permission_summary)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.permission_page_explanation)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.navigate_back_desc)).performClick()
        // Back returns to the explanation at the position it was left at.
        compose.onNodeWithText(label(R.string.shortcut_help_permission_summary)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.shortcut_help_intro)).performScrollTo().assertIsDisplayed()
    }
}
