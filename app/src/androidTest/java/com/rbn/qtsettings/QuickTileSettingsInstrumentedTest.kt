package com.rbn.qtsettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.DnsListSortMode
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.PermissionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

private const val TAG = "QuickTileSettingsTest"
private const val TAG_NETWORK_TYPE_DETECTION_TILE_OPTION = "network_type_detection_tile_only_option"
private const val TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION =
    "network_type_detection_background_option"
private const val TAG_NETWORK_TYPE_DETECTION_TOGGLE = "network_type_detection_toggle"
private const val TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL =
    "network_type_detection_wifi_state_label"
private const val TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL =
    "network_type_detection_mobile_state_label"

@RunWith(AndroidJUnit4::class)
class QuickTileSettingsInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private var hasPostNotificationsPermission = false
    private var canUseBackgroundDetection = false

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.waitForIdle()

        grantPostNotificationsPermissionIfNeeded()
        composeTestRule.dismissInitialDialogsIfPresent(context)

        canUseBackgroundDetection =
            hasPostNotificationsPermission && PermissionUtils.hasWriteSecureSettingsPermission(
                context
            )
    }

    @Test
    fun appLaunches_shouldShowMainScreenWithTabs() {
        // Verify app name in top bar
        composeTestRule.onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()

        // Verify app menu is present; permission help is available from that menu.
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.app_menu_desc))
            .assertIsDisplayed()

        // Verify both tabs are present
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb))
            .assertIsDisplayed()

        Log.i(TAG, "Main screen loaded successfully with all expected elements")
    }

    @Test
    fun tabNavigation_shouldWorkCorrectly() {
        // Start on DNS tab (should be default)
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()

        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        // Verify USB settings are visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        // Navigate back to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Verify DNS settings are visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))

        Log.i(TAG, "Tab navigation works correctly")
    }

    @Test
    fun permissionDialog_shouldShowAndDismiss() {
        // Click help button to show permission dialog
        composeTestRule.clickHelpButton(context)

        // Wait for permission dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.permission_grant_dialog_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify dialog is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.permission_grant_dialog_title))
            .assertIsDisplayed()

        // Verify dialog content
        composeTestRule.onNodeWithText(context.getString(R.string.permission_grant_dialog_intro))
            .assertIsDisplayed()

        // Dismiss dialog
        try {
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performScrollTo()
                .performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performClick()
        }
        composeTestRule.waitForIdle()

        Log.i(TAG, "Permission dialog displayed and dismissed successfully")
    }

    @Test
    fun dnsSettings_shouldShowAllOptions() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Verify DNS settings title
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))

        // Verify tile cycles description
        composeTestRule.onNodeWithText(context.getString(R.string.setting_desc_tile_cycles))
            .assertIsDisplayed()

        // Verify DNS state options exist
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_state_off))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_state_auto))

        Log.i(TAG, "DNS settings displayed correctly")
    }

    @Test
    fun usbDebuggingSettings_shouldShowAllOptions() {
        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        // Verify USB debugging settings title
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        // Verify tile cycles description
        composeTestRule.onNodeWithText(context.getString(R.string.setting_desc_tile_cycles))
            .assertIsDisplayed()

        // Verify USB state options exist
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_on))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.usb_state_off))

        Log.i(TAG, "USB debugging settings displayed correctly")
    }

    @Test
    fun permissionWarningCard_shouldShowWhenPermissionMissing() {
        // The permission warning card should be visible by default
        // since we don't have WRITE_SECURE_SETTINGS in test environment

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.warning_permission_missing_title))

        composeTestRule.onNodeWithText(context.getString(R.string.warning_permission_missing_desc))
            .assertIsDisplayed()

        // Click the permission help button on the warning card
        composeTestRule.onNodeWithText(context.getString(R.string.button_show_permission_help))
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "Permission warning card displayed and button works")
    }

    @Test
    fun adbInstructionsDialog_shouldShowWhenRequested() {
        // First open permission dialog
        composeTestRule.clickHelpButton(context)
        composeTestRule.waitForIdle()

        // Look for ADB instructions button
        try {
            composeTestRule.onNodeWithText(context.getString(R.string.button_show_adb_instructions))
                .performScrollTo()
                .performClick()
        } catch (_: AssertionError) {
            composeTestRule.onNodeWithText(context.getString(R.string.button_show_adb_instructions))
                .performClick()
        }
        composeTestRule.waitForIdle()

        // Verify ADB instructions dialog appears
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.adb_instructions_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(context.getString(R.string.adb_instructions_title))
            .assertIsDisplayed()

        Log.i(TAG, "ADB instructions dialog displayed successfully")
    }

    @Test
    fun multipleTabSwitches_shouldMaintainUIState() {
        // Perform multiple tab switches to ensure UI state is maintained
        repeat(3) { iteration ->
            Log.i(TAG, "Tab switch iteration ${iteration + 1}")

            // Switch to USB tab
            composeTestRule.navigateToUsbTab(context)

            // Verify USB content is visible
            composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

            // Switch to DNS tab
            composeTestRule.navigateToDnsTab(context)

            // Verify DNS content is visible
            composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))
        }

        Log.i(TAG, "Multiple tab switches completed successfully")
    }

    @Test
    fun uiElements_shouldBeAccessible() {
        // Verify accessibility of main UI elements

        // Top bar menu should have content description
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.app_menu_desc))
            .assertIsDisplayed()

        // Tab navigation should be accessible
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_dns))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tab_title_usb))
            .assertIsDisplayed()

        Log.i(TAG, "UI accessibility verification completed")
    }

    @Test
    fun dnsProviderInfoButtons_shouldOpenInfoDialogs() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        // Test info buttons for predefined DNS providers
        val predefinedProviders = listOf(
            "AdGuard DNS" to "dns.adguard.com",
            "Cloudflare (1.1.1.1)" to "one.one.one.one",
            "Quad9 Security" to "dns.quad9.net"
        )

        predefinedProviders.forEach { (providerName, providerHostname) ->
            Log.i(TAG, "Testing info button for: $providerName")

            // Open the compact row actions and select Info.
            try {
                composeTestRule.onNodeWithContentDescription(
                    context.getString(
                        R.string.dns_entry_actions,
                        providerName,
                        providerHostname
                    )
                )
                    .performScrollTo()
                    .performClick()
            } catch (_: AssertionError) {
                composeTestRule.onNodeWithContentDescription(
                    context.getString(
                        R.string.dns_entry_actions,
                        providerName,
                        providerHostname
                    )
                )
                    .performClick()
            }
            composeTestRule.onNodeWithText(
                context.getString(R.string.button_info_dns, providerName)
            ).performClick()
            composeTestRule.waitForIdle()

            // Wait for info dialog to appear
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                composeTestRule.onAllNodesWithText(
                    context.getString(
                        R.string.dns_info_dialog_title,
                        providerName
                    )
                )
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify dialog is displayed
            composeTestRule.onNodeWithText(
                context.getString(
                    R.string.dns_info_dialog_title,
                    providerName
                )
            )
                .assertIsDisplayed()

            // Close dialog
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
                .performClick()
            composeTestRule.waitForIdle()

            Log.i(TAG, "Info dialog for $providerName displayed and dismissed successfully")
        }
    }

    @Test
    fun dnsOffAndAutoRows_shouldOfferSetActiveAction() {
        composeTestRule.navigateToDnsTab(context)

        listOf(
            context.getString(R.string.dns_state_off),
            context.getString(R.string.dns_state_auto)
        ).forEach { modeLabel ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_set_active_mode, modeLabel)
            )
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun usbOnAndOffRows_shouldOfferSetActiveAction() {
        composeTestRule.navigateToUsbTab(context)

        listOf(
            context.getString(R.string.usb_state_on),
            context.getString(R.string.usb_state_off)
        ).forEach { stateLabel ->
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.usb_set_active_mode, stateLabel)
            )
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun dnsSortingMenu_shouldExposeManualReorderingWithoutAddingRowActions() {
        composeTestRule.navigateToDnsTab(context)

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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manualDnsReorder_shouldAnimateDisplacedRowsAndRestoreItsConfiguredOrder() {
        val preferencesManager = PreferencesManager.getInstance(context)
        val originalSortMode = preferencesManager.dnsListSortMode.value

        preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
        val originalManualOrder = preferencesManager.dnsHostnames.value.map { it.id }
        val entriesById = preferencesManager.dnsHostnames.value.associateBy { it.id }
        val adGuard = entriesById.getValue("adguard_default")
        val cloudflare = entriesById.getValue("cloudflare_default")
        val quad9 = entriesById.getValue("quad9_default")
        val controlledIds = setOf(adGuard.id, cloudflare.id, quad9.id)
        val controlledOrder =
            listOf(adGuard.id, cloudflare.id, quad9.id) +
                originalManualOrder.filterNot(controlledIds::contains)

        try {
            assertTrue(preferencesManager.reorderDnsHostnames(controlledOrder))

            composeTestRule.navigateToDnsTab(context)
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

            adGuardHandle.performScrollTo()
            composeTestRule.waitForIdle()

            val adGuardStartY = adGuardHandle.fetchSemanticsNode().boundsInRoot.top
            val cloudflareStartY = cloudflareHandle.fetchSemanticsNode().boundsInRoot.top
            assertTrue("The controlled DNS order was not shown", adGuardStartY < cloudflareStartY)

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
                    preferencesManager.dnsHostnames.value.take(3).map { it.id }
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
                preferencesManager.dnsHostnames.value.take(3).map { it.id }
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
                preferencesManager.dnsHostnames.value.take(3).map { it.id }
            )

            preferencesManager.setDnsListSortMode(DnsListSortMode.ALPHABETICAL)
            composeTestRule.waitForIdle()
            preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
            composeTestRule.waitForIdle()

            assertEquals(
                "Switching back to manual sorting did not restore the configured order",
                listOf(adGuard.id, quad9.id, cloudflare.id),
                preferencesManager.dnsHostnames.value.take(3).map { it.id }
            )
        } finally {
            composeTestRule.mainClock.autoAdvance = true
            preferencesManager.setDnsListSortMode(DnsListSortMode.MANUAL)
            preferencesManager.reorderDnsHostnames(originalManualOrder)
            preferencesManager.setDnsListSortMode(originalSortMode)
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun customDnsHostname_shouldSupportFullCRUD() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        val testRunId = System.nanoTime().toString()
        val customHostname = "Test Custom DNS $testRunId"
        val customValue = "dns-$testRunId.example.com"
        val editedHostname = "Edited Custom DNS $testRunId"
        val editedValue = "dns-edited-$testRunId.example.com"

        // CREATE: Add custom hostname
        Log.i(TAG, "Testing CREATE: Adding custom DNS hostname")

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
        Log.i(TAG, "Testing READ: Verifying custom hostname '$customHostname' appears in list")
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
        Log.i(TAG, "Testing UPDATE: Editing custom hostname to '$editedHostname'")
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
        Log.i(TAG, "Verifying edited hostname '$editedHostname' appears in list")
        composeTestRule.safeScrollToAndAssert(editedHostname)
        composeTestRule.safeScrollToAndAssert(editedValue)

        // DELETE: Remove the custom hostname
        Log.i(TAG, "Testing DELETE: Deleting custom hostname '$editedHostname'")
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
        Log.i(TAG, "Verifying hostname '$editedHostname' is removed from list")
        composeTestRule.onNodeWithText(editedHostname)
            .assertDoesNotExist()

        Log.i(TAG, "Custom DNS hostname CRUD test completed successfully")
    }

    @Test
    fun dnsSettings_shouldHandleCheckboxInteractions() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing DNS checkbox interactions")

        // Test DNS Off checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.dns_state_off))
            .performClick()
        composeTestRule.waitForIdle()

        // Test DNS Auto checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.dns_state_auto))
            .performClick()
        composeTestRule.waitForIdle()

        // Test VPN Detection checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_vpn_detection_enabled))
            .performClick()
        composeTestRule.waitForIdle()

        // Test Auto-Revert checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
            .performClick()
        composeTestRule.waitForIdle()

        // Test predefined DNS provider checkbox (uncheck AdGuard DNS)
        composeTestRule.onNodeWithText("AdGuard DNS")
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun usbSettings_shouldHandleCheckboxInteractions() {
        // Navigate to USB tab
        composeTestRule.navigateToUsbTab(context)

        Log.i(TAG, "Testing USB checkbox interactions")

        // Test USB Enable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_on))
            .performClick()
        composeTestRule.waitForIdle()

        // Test USB Disable checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.usb_state_off))
            .performClick()
        composeTestRule.waitForIdle()

        // Test USB Auto-Revert checkbox
        composeTestRule.onNodeWithText(context.getString(R.string.setting_enable_auto_revert))
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun networkTypeDetection_shouldShowInDnsSettings() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection visibility in DNS settings")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Verify Network Type Detection info button is present
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.network_type_info_title)
        )
            .assertIsDisplayed()

        // Note: Description text may not be in semantic tree, so we just verify main elements exist
        Log.i(TAG, "Network Type Detection section displayed successfully")
    }

    @Test
    fun networkTypeDetection_shouldHandleEnableDisableToggle() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection enable/disable toggle")

        // Scroll to Network Type Detection checkbox
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        val networkTypeInitiallyEnabled = composeTestRule.onAllNodesWithText(
            context.getString(R.string.setting_dns_state_on_wifi)
        ).fetchSemanticsNodes().isNotEmpty()

        if (networkTypeInitiallyEnabled) {
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Click to enable Network Type Detection
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify WiFi DNS state selector appears when enabled
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_WIFI_STATE_LABEL)
            .performScrollTo()
            .assertIsDisplayed()

        // Verify Mobile DNS state selector appears when enabled
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_MOBILE_STATE_LABEL)
            .performScrollTo()
            .assertIsDisplayed()

        // Click to disable Network Type Detection
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TOGGLE)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "Network Type Detection toggle works correctly")
    }

    @Test
    fun networkTypeDetection_dnsStateSelectorsShouldWork() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection DNS state selectors")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        ensureNetworkTypeDetectionEnabled()

        // Test WiFi DNS State selector
        Log.i(TAG, "Testing WiFi DNS state selector")
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_wifi))

        // Click the WiFi DNS state dropdown
        composeTestRule.onNodeWithText(context.getString(R.string.setting_dns_state_on_wifi))
            .performScrollTo()
        composeTestRule.waitForIdle()

        // Verify the WiFi DNS state selector is displayed
        composeTestRule.onNodeWithText(
            context.getString(R.string.setting_dns_state_on_wifi),
            substring = true
        )
            .assertIsDisplayed()

        // Test Mobile Data DNS State selector
        Log.i(TAG, "Testing Mobile Data DNS state selector")
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_mobile))

        // Click the Mobile DNS state dropdown
        composeTestRule.onNodeWithText(context.getString(R.string.setting_dns_state_on_mobile))
            .performScrollTo()
        composeTestRule.waitForIdle()

        // Verify the current selection shows "Auto" by default
        composeTestRule.onNodeWithText(
            context.getString(R.string.setting_dns_state_on_mobile),
            substring = true
        )
            .assertIsDisplayed()

        Log.i(TAG, "DNS state selectors displayed correctly")
    }

    @Test
    fun networkTypeDetection_detectionModeShouldBeSelectable() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection mode selection")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        ensureNetworkTypeDetectionEnabled()

        // Scroll to detection mode section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_mode))

        // Verify tile-only mode is visible
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TILE_OPTION)
            .performScrollTo()
            .assertIsDisplayed()

        // Verify background mode is visible
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
            .performScrollTo()
            .assertIsDisplayed()

        // Test clicking background mode (requires notification permission on Android 13+)
        if (canUseBackgroundDetection) {
            Log.i(TAG, "Testing background mode selection")
            composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_BACKGROUND_OPTION)
                .performClick()
            composeTestRule.waitForIdle()
        } else {
            Log.w(
                TAG,
                "Skipping background mode selection; required permissions unavailable"
            )
        }

        // Test clicking tile-only mode
        Log.i(TAG, "Testing tile-only mode selection")
        composeTestRule.onNodeWithTag(TAG_NETWORK_TYPE_DETECTION_TILE_OPTION)
            .performClick()
        composeTestRule.waitForIdle()

        Log.i(TAG, "Detection mode selection works correctly")
    }

    @Test
    fun networkTypeInfoDialog_shouldDisplayAndDismiss() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection info dialog")

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Click the info button
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.network_type_info_title)
        )
            .performClick()
        composeTestRule.waitForIdle()

        // Wait for info dialog to appear
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.network_type_info_title))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify dialog title is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_title))
            .assertIsDisplayed()

        // Verify dialog message is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_message))
            .assertIsDisplayed()

        // Close the dialog
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_close))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText(context.getString(R.string.network_type_info_title))
            .assertDoesNotExist()

        Log.i(TAG, "Network Type Detection info dialog displayed and dismissed successfully")
    }

    @Test
    fun networkTypeDetection_shouldCoexistWithVpnDetection() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing Network Type Detection and VPN Detection coexistence")

        // Verify VPN Detection section is visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_vpn_detection_enabled))

        // Check if VPN Detection is already enabled, if not enable it
        // If VPN detection mode label doesn't exist, VPN detection is off
        val vpnAlreadyEnabled = try {
            composeTestRule.onAllNodesWithText(context.getString(R.string.setting_vpn_detection_mode))
                .fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
            false
        }

        if (!vpnAlreadyEnabled) {
            Log.i(TAG, "Enabling VPN Detection")
            composeTestRule.onNodeWithText(context.getString(R.string.setting_vpn_detection_enabled))
                .performClick()
            composeTestRule.waitForIdle()
        } else {
            Log.i(TAG, "VPN Detection already enabled")
        }

        // Scroll to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        ensureNetworkTypeDetectionEnabled()

        // Verify both sections are enabled and visible
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_vpn_detection_enabled))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))

        // Verify Network Type Detection options are visible when enabled
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_wifi))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_mobile))

        Log.i(TAG, "Network Type Detection and VPN Detection coexist correctly")
    }

    @Test
    fun networkTypeDetection_shouldShowAllDnsStateOptions() {
        // Navigate to DNS tab
        composeTestRule.navigateToDnsTab(context)

        Log.i(TAG, "Testing DNS state options in Network Type Detection")

        // Enable Network Type Detection
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_network_type_detection_enabled))
        composeTestRule.onNodeWithText(context.getString(R.string.setting_network_type_detection_enabled))
            .performClick()
        composeTestRule.waitForIdle()

        // Add a custom DNS hostname first to test hostname option
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.dns_add_custom_hostname_button))
        composeTestRule.onNodeWithText(context.getString(R.string.dns_add_custom_hostname_button))
            .performClick()
        composeTestRule.waitForIdle()

        // Fill in custom hostname
        val testHostname = "Test Network Type DNS"
        val testValue = "dns.networktype.test"
        composeTestRule.onAllNodes(hasSetTextAction())[0]
            .performTextInput(testHostname)
        composeTestRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput(testValue)

        // Save the hostname
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_save))
            .performClick()
        composeTestRule.waitForIdle()

        // Verify custom hostname is added
        composeTestRule.safeScrollToAndAssert(testHostname)

        // Scroll back to Network Type Detection section
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_wifi))

        // Verify WiFi DNS state label is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.setting_dns_state_on_wifi))
            .assertIsDisplayed()

        // Verify Mobile DNS state label is displayed
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_dns_state_on_mobile))
        composeTestRule.onNodeWithText(context.getString(R.string.setting_dns_state_on_mobile))
            .assertIsDisplayed()

        // Clean up: delete the test hostname
        try {
            composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.dns_entry_actions, testHostname, testValue)
            )
                .performScrollTo()
                .performClick()
            composeTestRule.onNodeWithTag("dns_delete_button_${testValue}_${testHostname}")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(context.getString(R.string.button_delete))
                .performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // If cleanup fails, log it but don't fail the test
            Log.w(TAG, "Failed to clean up test hostname: ${e.message}")
        }

        Log.i(TAG, "DNS state options in Network Type Detection work correctly")
    }

    private fun ensureNetworkTypeDetectionEnabled() {
        val networkTypeAlreadyEnabled = try {
            composeTestRule.onAllNodesWithText(context.getString(R.string.setting_dns_state_on_wifi))
                .fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
            false
        }

        if (!networkTypeAlreadyEnabled) {
            Log.i(TAG, "Enabling Network Type Detection")
            composeTestRule.onNodeWithText(context.getString(R.string.setting_network_type_detection_enabled))
                .performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500) // Give animation time to complete
        } else {
            Log.i(TAG, "Network Type Detection already enabled")
        }
    }

    private fun grantPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            hasPostNotificationsPermission = true
            return
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val packageName = instrumentation.targetContext.packageName

        val alreadyGranted = ContextCompat.checkSelfPermission(
            instrumentation.targetContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            hasPostNotificationsPermission = true
            Log.i(TAG, "POST_NOTIFICATIONS permission already granted")
            return
        }

        runCatching {
            instrumentation.uiAutomation.executeShellCommand("pm grant $packageName $permission")
                .use { fd ->
                    drainCommandOutput(fd)
                }
            instrumentation.waitForIdleSync()
        }.onFailure { error ->
            Log.w(TAG, "Failed to grant POST_NOTIFICATIONS permission: ${error.message}")
        }

        hasPostNotificationsPermission = ContextCompat.checkSelfPermission(
            instrumentation.targetContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun drainCommandOutput(fd: ParcelFileDescriptor) {
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                while (reader.readLine() != null) {
                    // Drain output to ensure command completes
                }
            }
        }
    }
}
