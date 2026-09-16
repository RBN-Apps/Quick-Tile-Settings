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

    @Test fun globalEntry_expandedInfoAndSettingsRemainScrollable() {
        compose.navigateToShortcutSettings(context)
        val pinnedFallback = label(R.string.shortcut_settings_allow_pinned_when_disabled)
        compose.onNodeWithText(pinnedFallback).assertIsDisplayed()
        assertGlobalCounts()
        val config = AppStores.getInstance(context).shortcuts.state.value
        compose.onNodeWithText(context.getString(R.string.shortcut_settings_limit,
            config.enabledIds.size, config.maxCount)).performClick()
        val description = context.getString(R.string.shortcut_settings_description, config.maxCount,
            ShortcutCatalog.MAX_FAVORITE_SHORTCUTS)
        compose.onNodeWithText(description).assertExists()
        compose.onNodeWithText(pinnedFallback).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(description).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(description).assertDoesNotExist()
    }
}
