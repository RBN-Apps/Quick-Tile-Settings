package com.rbn.qtsettings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule

/**
 * Extension functions for AndroidComposeTestRule to help with testing
 */
fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.dismissInitialDialogsIfPresent(
    context: Context
) {
    try {
        // Try to dismiss permission dialog if it appears
        waitUntil(timeoutMillis = 2_000) {
            onAllNodesWithText(context.getString(R.string.dialog_close))
                .fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText(context.getString(R.string.dialog_close))
            .performClick()
        waitForIdle()
    } catch (_: ComposeTimeoutException) {
        // No dialog present, continue
    }
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToTab(
    context: Context,
    tabName: String
) {
    try {
        onNodeWithText(tabName)
            .performScrollTo()
            .performClick()
    } catch (_: AssertionError) {
        // Try without scrolling if element is already visible
        onNodeWithText(tabName)
            .performClick()
    }
    waitForIdle()
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToDnsTab(
    context: Context
) {
    navigateToTab(context, context.getString(R.string.tab_title_dns))
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.navigateToUsbTab(
    context: Context
) {
    navigateToTab(context, context.getString(R.string.tab_title_usb))
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.clickHelpButton(
    context: Context
) {
    onNodeWithContentDescription(context.getString(R.string.help_button_desc))
        .performClick()
    waitForIdle()
}

fun <T : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<T>, T>.clickApplySettings(
    context: Context
) {
    try {
        onNodeWithText(context.getString(R.string.button_save_apply_settings))
            .performScrollTo()
            .performClick()
    } catch (_: AssertionError) {
        // Try without scrolling if element is already visible
        onNodeWithText(context.getString(R.string.button_save_apply_settings))
            .performClick()
    }
    waitForIdle()
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