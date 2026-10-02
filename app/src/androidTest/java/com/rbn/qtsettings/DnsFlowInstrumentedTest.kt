package com.rbn.qtsettings

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.app.AppStores
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DnsFlowInstrumentedTest : AppFlowTestFixture() {

    @Test
    fun providersPage_explainsTheCycle_andOffersActivationForOffAndAuto() {
        composeTestRule.navigateToDnsProviders(context)
        composeTestRule.onNodeWithText(context.getString(R.string.dns_cycle_explanation))
            .assertIsDisplayed()
        listOf(R.string.dns_state_off, R.string.dns_state_auto).forEach { mode ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_set_active_mode, context.getString(mode))
            ).performScrollTo().assertIsDisplayed()
        }
    }

    /** One provider suffices: the dialog title is formatted from the name on a shared code path. */
    @Test
    fun providerInfoAction_opensAndClosesItsInfoDialog() {
        composeTestRule.navigateToDnsProviders(context)
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.dns_entry_actions, "AdGuard DNS", "dns.adguard.com")
        ).performScrollTo().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.button_info_dns, "AdGuard DNS"))
            .performClick()

        val title = context.getString(R.string.dns_info_dialog_title, "AdGuard DNS")
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_close)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun dnsSortingMenu_shouldExposeManualReorderingWithoutAddingRowActions() {
        composeTestRule.navigateToDnsProviders(context)

        composeTestRule.onNodeWithTag("dns_sort_menu_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("dns_sort_manual").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(context.getString(R.string.dns_manual_reorder_hint))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            context.getString(
                R.string.dns_reorder_entry,
                "AdGuard DNS",
                "dns.adguard.com"
            )
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag("dns_sort_menu_button").performClick()
        composeTestRule.onNodeWithTag("dns_sort_alphabetical").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(context.getString(R.string.dns_manual_reorder_hint))
            .assertDoesNotExist()
    }

    @Test
    fun manualDnsReorder_shouldAnimateDisplacedRowsAndRestoreItsConfiguredOrder() {
        val preferencesManager = AppStores.getInstance(context)
        val originalSortMode = preferencesManager.dns.dnsListSortMode.value

        preferencesManager.dns.setDnsListSortMode(DnsListSortMode.MANUAL)
        val originalManualOrder = preferencesManager.dns.dnsHostnames.value.map { it.id }
        val entriesById = preferencesManager.dns.dnsHostnames.value.associateBy { it.id }
        val adGuard = entriesById.getValue("adguard_default")
        val cloudflare = entriesById.getValue("cloudflare_default")
        val quad9 = entriesById.getValue("quad9_default")
        val controlledIds = setOf(adGuard.id, cloudflare.id, quad9.id)
        val controlledOrder =
            listOf(adGuard.id, cloudflare.id, quad9.id) +
                originalManualOrder.filterNot(controlledIds::contains)

        try {
            assertTrue(preferencesManager.dns.reorderDnsHostnames(controlledOrder))

            composeTestRule.navigateToDnsProviders(context)
            composeTestRule.waitForIdle()

            val adGuardHandle = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_reorder_entry, adGuard.name, adGuard.hostname),
                useUnmergedTree = true
            )
            val cloudflareHandle = composeTestRule.onNodeWithContentDescription(
                context.getString(
                    R.string.dns_reorder_entry,
                    cloudflare.name,
                    cloudflare.hostname
                ),
                useUnmergedTree = true
            )
            val quad9Handle = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_reorder_entry, quad9.name, quad9.hostname),
                useUnmergedTree = true
            )

            // Position/drag assertions need all three rows in the viewport. Scrolling
            // only AdGuard can leave Cloudflare clipped (boundsInRoot.top == 0) on phones.
            quad9Handle.performScrollTo()
            adGuardHandle.assertIsDisplayed()
            cloudflareHandle.assertIsDisplayed()
            quad9Handle.assertIsDisplayed()
            composeTestRule.waitForIdle()

            val adGuardStartY = adGuardHandle.fetchSemanticsNode().boundsInRoot.top
            val cloudflareStartY = cloudflareHandle.fetchSemanticsNode().boundsInRoot.top
            assertTrue(
                "The controlled DNS order was not shown (AdGuard=$adGuardStartY, Cloudflare=$cloudflareStartY)",
                adGuardStartY < cloudflareStartY
            )

            composeTestRule.mainClock.autoAdvance = false
            adGuardHandle.performCustomAccessibilityActionWithLabel(
                context.getString(R.string.dns_move_down)
            )

            var sawIntermediatePosition = false
            repeat(24) {
                composeTestRule.mainClock.advanceTimeByFrame()
                val currentCloudflareY = cloudflareHandle.fetchSemanticsNode().boundsInRoot.top
                if (
                    currentCloudflareY < cloudflareStartY - 0.5f &&
                    currentCloudflareY > adGuardStartY + 0.5f
                ) {
                    sawIntermediatePosition = true
                }
            }

            assertTrue(
                "The displaced DNS row jumped instead of animating between positions",
                sawIntermediatePosition
            )

            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.waitForIdle()

            val adGuardEndY = adGuardHandle.fetchSemanticsNode().boundsInRoot.top
            val cloudflareEndY = cloudflareHandle.fetchSemanticsNode().boundsInRoot.top
            assertTrue("The DNS rows were not reordered", cloudflareEndY < adGuardEndY)
            assertTrue(
                "The displaced DNS row did not finish in the previous row slot",
                abs(cloudflareEndY - adGuardStartY) < 2f
            )

            val dragDistance = (cloudflareEndY - adGuardEndY) * 0.75f
            assertTrue("The DNS rows did not have a usable drag distance", dragDistance < 0f)

            adGuardHandle.performTouchInput {
                val start = center
                val holdDuration = viewConfiguration.longPressTimeoutMillis + 100L
                down(start)

                var heldFor = 0L
                while (heldFor < holdDuration) {
                    val eventDelay = minOf(eventPeriodMillis, holdDuration - heldFor)
                    move(eventDelay)
                    heldFor += eventDelay
                }

                assertEquals(
                    "Long-pressing the DNS row without moving must not reorder it",
                    listOf(cloudflare.id, adGuard.id, quad9.id),
                    preferencesManager.dns.dnsHostnames.value.take(3).map { it.id }
                )

                repeat(10) { step ->
                    val progress = (step + 1) / 10f
                    updatePointerTo(
                        pointerId = 0,
                        position = start + Offset(0f, dragDistance * progress)
                    )
                    move(20L)
                }
                up()
            }
            composeTestRule.waitForIdle()

            assertEquals(
                listOf(adGuard.id, cloudflare.id, quad9.id),
                preferencesManager.dns.dnsHostnames.value.take(3).map { it.id }
            )
            val adGuardAfterDragY = adGuardHandle.fetchSemanticsNode().boundsInRoot.top
            val cloudflareAfterDragY = cloudflareHandle.fetchSemanticsNode().boundsInRoot.top
            assertTrue(
                "The pointer drag did not place the DNS row above its neighbor",
                adGuardAfterDragY < cloudflareAfterDragY
            )

            quad9Handle.performCustomAccessibilityActionWithLabel(
                context.getString(R.string.dns_move_up)
            )
            composeTestRule.waitForIdle()
            assertEquals(
                listOf(adGuard.id, quad9.id, cloudflare.id),
                preferencesManager.dns.dnsHostnames.value.take(3).map { it.id }
            )

            preferencesManager.dns.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)
            composeTestRule.waitForIdle()
            preferencesManager.dns.setDnsListSortMode(DnsListSortMode.MANUAL)
            composeTestRule.waitForIdle()

            assertEquals(
                "Switching back to manual sorting did not restore the configured order",
                listOf(adGuard.id, quad9.id, cloudflare.id),
                preferencesManager.dns.dnsHostnames.value.take(3).map { it.id }
            )
        } finally {
            composeTestRule.mainClock.autoAdvance = true
            preferencesManager.dns.setDnsListSortMode(DnsListSortMode.MANUAL)
            preferencesManager.dns.reorderDnsHostnames(originalManualOrder)
            preferencesManager.dns.setDnsListSortMode(originalSortMode)
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun customDnsHostname_shouldSupportFullCRUD() {
        // Navigate to DNS area
        composeTestRule.navigateToDnsProviders(context)

        val testRunId = System.nanoTime().toString()
        val customHostname = "Test Custom DNS $testRunId"
        val customValue = "dns-$testRunId.example.com"
        val editedHostname = "Edited Custom DNS $testRunId"
        val editedValue = "dns-edited-$testRunId.example.com"

        // CREATE: Add custom hostname
        Log.i(FLOW_TAG, "Testing CREATE: Adding custom DNS hostname")

        // Click add button
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_add_custom_hostname_button))
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_custom_hostname_button))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify add dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_hostname_title))
            .assertIsDisplayed()

        // Fill in hostname form fields
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("  $customHostname  ")
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput("  $customValue  ")

        // Save the hostname
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // READ: Verify custom hostname appears in list
        Log.i(FLOW_TAG, "Testing READ: Verifying custom hostname '$customHostname' appears in list")
        composeTestRule.safeScrollToAndAssert(customHostname)
        composeTestRule.safeScrollToAndAssert(customValue)
        composeTestRule.onNode(
            hasText(customHostname) and
                hasText(context.getString(R.string.dns_custom_entry_badge))
        ).assertIsDisplayed()
        composeTestRule.safeScrollToAndAssert("AdGuard DNS")
        composeTestRule.onNode(
            hasText("AdGuard DNS") and
                hasText(context.getString(R.string.dns_custom_entry_badge))
        ).assertDoesNotExist()

        // UPDATE: Edit the custom hostname
        Log.i(FLOW_TAG, "Testing UPDATE: Editing custom hostname to '$editedHostname'")
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.dns_entry_actions, customHostname, customValue)
        ).performClick()
        composeTestRule.onNodeWithTag("dns_edit_button_${customValue}_${customHostname}")
            .performClick()
        composeTestRule.waitForIdle()

        // Verify edit dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.dns_edit_hostname_title))
            .assertIsDisplayed()

        // Clear and update fields with edited values
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextClearance()
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput("  $editedHostname  ")
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextClearance()
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput("  $editedValue  ")

        // Save the changes
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify edited hostname appears in list
        Log.i(FLOW_TAG, "Verifying edited hostname '$editedHostname' appears in list")
        composeTestRule.safeScrollToAndAssert(editedHostname)
        composeTestRule.safeScrollToAndAssert(editedValue)

        // DELETE: Remove the custom hostname
        Log.i(FLOW_TAG, "Testing DELETE: Deleting custom hostname '$editedHostname'")
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.dns_entry_actions, editedHostname, editedValue)
        ).performClick()
        composeTestRule.onNodeWithTag("dns_delete_button_${editedValue}_${editedHostname}")
            .performClick()
        composeTestRule.waitForIdle()

        // Confirm deletion dialog appears
        composeTestRule.onNodeWithText(context.getString(R.string.confirm_delete_hostname_title))
            .assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithText(context.getString(R.string.button_delete))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify hostname is removed from list
        Log.i(FLOW_TAG, "Verifying hostname '$editedHostname' is removed from list")
        composeTestRule.onNodeWithText(editedHostname)
            .assertDoesNotExist()

        Log.i(FLOW_TAG, "Custom DNS hostname CRUD test completed successfully")
    }

    /** The installed app wires the DNS pages to the real store; the screens are covered by DnsUi. */
    @Test
    fun providersAndRevertPages_persistTheirSelections() {
        val store = AppStores.getInstance(context).dns
        val originalOff = store.dnsToggleOff.value
        val originalRevert = store.dnsEnableAutoRevert.value
        try {
            composeTestRule.navigateToDnsProviders(context)
            composeTestRule.onNodeWithText(context.getString(R.string.dns_state_off))
                .performScrollTo().performClick()
            composeTestRule.runOnIdle { assertEquals(!originalOff, store.dnsToggleOff.value) }

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigate_back_desc))
                .performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.usb_menu_revert))
                .performScrollTo().performClick()
            composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
                .performClick()
            composeTestRule.runOnIdle { assertEquals(!originalRevert, store.dnsEnableAutoRevert.value) }
        } finally {
            store.setDnsToggleOff(originalOff)
            store.setDnsEnableAutoRevert(originalRevert)
        }
    }
}
