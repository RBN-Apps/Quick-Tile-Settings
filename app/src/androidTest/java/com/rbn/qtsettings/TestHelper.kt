package com.rbn.qtsettings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.rbn.qtsettings.app.navigation.AppRailTarget
import com.rbn.qtsettings.app.navigation.AppArea

/**
 * Extension functions for AndroidComposeTestRule to help with testing
 */
fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.dismissInitialDialogsIfPresent(
    context: Context
) {
    try {
        // Initial help is a page; return without changing the permission.
        waitUntil(timeoutMillis = 2_000) {
            onAllNodesWithText(context.getString(R.string.permission_page_explanation))
                .fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription(context.getString(R.string.navigate_back_desc))
            .performClick()
        waitForIdle()
    } catch (_: ComposeTimeoutException) {
        // No dialog present, continue
    }
}

/** Open the modal rail only when the compact window hides its destinations. */
fun ComposeTestRule.openNavigationIfNeeded(context: Context): Boolean {
    val compact = onAllNodesWithTag("persistent_navigation_rail").fetchSemanticsNodes().isEmpty()
    if (compact) onNodeWithContentDescription(context.getString(R.string.navigation_open)).performClick()
    return compact
}

fun ComposeTestRule.selectNavigationArea(context: Context, area: AppArea) {
    openNavigationIfNeeded(context)
    onNodeWithTag("navigation_area_${area.name}").performClick()
    waitForIdle()
}

fun ComposeTestRule.assertNavigationAreaSelected(context: Context, area: AppArea) {
    val compact = openNavigationIfNeeded(context)
    onNodeWithTag("navigation_area_${area.name}").assertIsSelected()
    // Reselection closes the compact overlay without changing the Navigation 3 history.
    if (compact) onNodeWithTag("navigation_area_${area.name}").performClick()
    waitForIdle()
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToDnsArea(context: Context) {
    selectNavigationArea(context, AppArea.Dns)
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToUsbArea(context: Context) {
    selectNavigationArea(context, AppArea.Usb)
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.clickHelpButton(
    context: Context
) {
    selectNavigationTarget(context, AppRailTarget.Permissions)
}

/**
 * Safely perform scrollTo and assert, falling back to just assert if scrolling fails
 */
fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.safeScrollToAndAssert(
    text: String
) {
    try {
        onNodeWithText(text)
            .performScrollTo()
            .assertIsDisplayed()
    } catch (_: AssertionError) {
        // Try without scrolling if element is already visible
        onNodeWithText(text)
            .assertIsDisplayed()
    }
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToShortcutSettings(
    context: Context
) {
    selectNavigationTarget(context, AppRailTarget.Shortcuts)
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateBackFromShortcutSettings(
    context: Context
) {
    onNodeWithContentDescription(context.getString(R.string.navigate_back_desc))
        .performClick()
    waitForIdle()
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToDnsProviders(context: Context) {
    navigateToDnsArea(context)
    onNodeWithText(context.getString(R.string.dns_menu_providers)).performScrollTo().performClick()
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToDnsAutomation(context: Context) {
    navigateToDnsArea(context)
    onNodeWithText(context.getString(R.string.dns_menu_automation)).performScrollTo().performClick()
}

fun ComposeTestRule.selectNavigationTarget(context: Context, target: AppRailTarget) {
    openNavigationIfNeeded(context)
    onNodeWithTag(target.testTag).performScrollTo().performClick()
    waitForIdle()
}

fun ComposeTestRule.assertNavigationAvailable(context: Context) {
    if (onAllNodesWithTag("persistent_navigation_rail").fetchSemanticsNodes().isEmpty())
        onNodeWithContentDescription(context.getString(R.string.navigation_open)).assertIsDisplayed()
    else onNodeWithTag("persistent_navigation_rail").assertIsDisplayed()
}
