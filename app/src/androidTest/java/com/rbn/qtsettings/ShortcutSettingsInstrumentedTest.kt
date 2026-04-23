package com.rbn.qtsettings

import android.content.Context
import android.content.pm.ShortcutManager
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.ShortcutUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "ShortcutSettingsTest"

@RunWith(AndroidJUnit4::class)
class ShortcutSettingsInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.waitForIdle()
        composeTestRule.dismissInitialDialogsIfPresent(context)
        val prefsManager = PreferencesManager.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.dnsHostnames.value
                .filter { !it.isPredefined }
                .forEach { prefsManager.deleteCustomDnsHostname(it.id) }
            val allAvailableIds =
                ShortcutUtils.getAvailableShortcutIds(prefsManager.dnsHostnames.value)
            prefsManager.setAllowPinnedShortcutsWhenDisabled(false)
            allAvailableIds.forEach { prefsManager.setShortcutExposureEnabled(it, false) }
            allAvailableIds.forEach { prefsManager.setShortcutFavorite(it, false) }
        }
    }

    @After
    fun tearDown() {
        // Remove any custom DNS entries added during limit tests so they don't leak into other tests
        val prefsManager = PreferencesManager.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.dnsHostnames.value
                .filter { !it.isPredefined }
                .forEach { prefsManager.deleteCustomDnsHostname(it.id) }
            val allAvailableIds =
                ShortcutUtils.getAvailableShortcutIds(prefsManager.dnsHostnames.value)
            prefsManager.setAllowPinnedShortcutsWhenDisabled(false)
            allAvailableIds.forEach { prefsManager.setShortcutExposureEnabled(it, false) }
            allAvailableIds.forEach { prefsManager.setShortcutFavorite(it, false) }
        }
    }

    // === Navigation ===

    @Test
    fun shortcutSettings_backButton_shouldReturnToMainScreen() {
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.shortcut_settings_open_desc)
        ).assertIsDisplayed()

        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.shortcut_settings_back_desc)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_title))
            .assertIsDisplayed()

        composeTestRule.navigateBackFromShortcutSettings(context)

        // Main screen app name is visible again; shortcut settings title is gone
        composeTestRule.onNodeWithText(context.getString(R.string.app_name))
            .assertIsDisplayed()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(context.getString(R.string.shortcut_settings_title))
                .fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.shortcut_settings_open_desc)
        ).assertIsDisplayed()

        Log.i(TAG, "Back button returns to the main screen")
    }

    // === Screen content ===

    @Test
    fun shortcutSettings_shouldShowActiveShortcutCountIndicator() {
        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText("Active shortcuts", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Active shortcuts", substring = true)
            .assertIsDisplayed()

        Log.i(TAG, "Active shortcut count indicator is visible")
    }

    @Test
    fun shortcutSettings_shouldShowPrivateDnsGroupTitle() {
        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_private_dns))

        Log.i(TAG, "Private DNS group title is visible")
    }

    @Test
    fun shortcutSettings_shouldShowUsbDebuggingGroupTitle() {
        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.setting_title_usb_debugging))

        Log.i(TAG, "USB Debugging group title is visible")
    }

    @Test
    fun shortcutSettings_shouldShowAllPredefinedDnsShortcutItems() {
        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_dns_off_short))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_dns_auto_short))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_dns_adguard_short))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_dns_cloudflare_short))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_dns_quad9_short))

        Log.i(TAG, "All predefined DNS shortcut items are visible")
    }

    @Test
    fun shortcutSettings_shouldShowAllUsbShortcutItems() {
        composeTestRule.navigateToShortcutSettings(context)

        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_usb_on_short))
        composeTestRule.safeScrollToAndAssert(context.getString(R.string.shortcut_usb_off_short))

        Log.i(TAG, "USB shortcut items are visible")
    }

    // === Toggle interaction ===

    @Test
    fun shortcutSettings_disablingShortcut_shouldDecreaseActiveCount() {
        val dnsOffId = ShortcutUtils.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)

        // Pre-condition: ensure DNS Off is enabled so disabling it decreases the count
        val prefsManager = PreferencesManager.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setShortcutExposureEnabled(dnsOffId, true)
        }
        composeTestRule.waitForIdle()

        val countBefore = prefsManager.enabledShortcutIds.value.size

        composeTestRule.navigateToShortcutSettings(context)
        composeTestRule.safeScrollToAndAssert(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        val countAfter = prefsManager.enabledShortcutIds.value.size
        assertEquals(
            "Active count should decrease by 1 after disabling (before=$countBefore, after=$countAfter)",
            countBefore - 1,
            countAfter
        )

        Log.i(TAG, "Disabling a shortcut decreases active count from $countBefore to $countAfter")
    }

    @Test
    fun shortcutSettings_enablingShortcut_shouldIncreaseActiveCount() {
        val dnsOffId = ShortcutUtils.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)

        // Pre-condition: ensure DNS Off is disabled so enabling it increases the count
        val prefsManager = PreferencesManager.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setShortcutExposureEnabled(dnsOffId, false)
        }
        composeTestRule.waitForIdle()

        val countBefore = prefsManager.enabledShortcutIds.value.size

        composeTestRule.navigateToShortcutSettings(context)
        composeTestRule.safeScrollToAndAssert(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        // If limit was reached, the dialog appears; dismiss it and the count stays the same
        val dialogNodes = composeTestRule
            .onAllNodesWithText(context.getString(R.string.shortcut_settings_limit_reached_title))
            .fetchSemanticsNodes()
        if (dialogNodes.isNotEmpty()) {
            Log.i(TAG, "Limit already reached; dismissing dialog — count unchanged is expected")
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close)).performClick()
            composeTestRule.waitForIdle()
            return
        }

        val countAfter = prefsManager.enabledShortcutIds.value.size
        assertEquals(
            "Active count should increase by 1 after enabling (before=$countBefore, after=$countAfter)",
            countBefore + 1,
            countAfter
        )

        Log.i(TAG, "Enabling a shortcut increases active count from $countBefore to $countAfter")
    }

    // === Limit-reached dialog ===

    @Test
    fun shortcutSettings_limitReachedDialog_shouldDisplayAndDismiss() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val maxCount = shortcutManager?.maxShortcutCountPerActivity ?: 5

        // Ensure there are more shortcuts available than the system limit so the limit can be hit
        ensureEnoughShortcutsForLimit(maxCount)

        val prefsManager = PreferencesManager.getInstance(context)
        val allOrderedIds = ShortcutUtils.getOrderedShortcutIds(prefsManager.dnsHostnames.value)
        val idToTitle = buildIdToTitleMap()

        check(allOrderedIds.size > maxCount) {
            "Expected more than $maxCount shortcuts after adding custom entries, " +
                    "but got ${allOrderedIds.size}"
        }

        // Set up known state: disable all, then enable exactly maxCount shortcuts
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            allOrderedIds.forEach { id -> prefsManager.setShortcutExposureEnabled(id, false) }
            allOrderedIds.take(maxCount).forEach { id ->
                prefsManager.setShortcutExposureEnabled(id, true)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.navigateToShortcutSettings(context)

        // The (maxCount+1)th shortcut is disabled while at the limit -> clicking it triggers dialog
        val disabledId = allOrderedIds[maxCount]
        val disabledTitle = checkNotNull(idToTitle[disabledId]) {
            "No UI title found for shortcut id '$disabledId'"
        }
        Log.i(TAG, "Clicking '$disabledTitle' to trigger limit-reached dialog")

        composeTestRule.safeScrollToAndAssert(disabledTitle)
        composeTestRule.onNodeWithText(disabledTitle).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(
                context.getString(R.string.shortcut_settings_limit_reached_title)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.shortcut_settings_limit_reached_title)
        ).assertIsDisplayed()

        composeTestRule.onNodeWithText(
            context.getString(R.string.shortcut_settings_limit_reached)
        ).assertIsDisplayed()

        // Dismiss via close button
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_close)).performClick()
        composeTestRule.waitForIdle()

        // Dialog must be gone
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(
                context.getString(R.string.shortcut_settings_limit_reached_title)
            ).fetchSemanticsNodes().isEmpty()
        }

        Log.i(TAG, "Limit-reached dialog displayed and dismissed successfully")
    }

    @Test
    fun shortcutSettings_limitReachedDialog_shouldNotShowWhenDisablingEnabledShortcut() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val maxCount = shortcutManager?.maxShortcutCountPerActivity ?: 5

        // Ensure there are more shortcuts available than the system limit so the limit can be hit
        ensureEnoughShortcutsForLimit(maxCount)

        val prefsManager = PreferencesManager.getInstance(context)
        val allOrderedIds = ShortcutUtils.getOrderedShortcutIds(prefsManager.dnsHostnames.value)

        check(allOrderedIds.size > maxCount) {
            "Expected more than $maxCount shortcuts after adding custom entries, " +
                    "but got ${allOrderedIds.size}"
        }

        // Fill shortcuts to the limit; DNS Off (index 0) is always among the enabled ones
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            allOrderedIds.forEach { id -> prefsManager.setShortcutExposureEnabled(id, false) }
            allOrderedIds.take(maxCount).forEach { id ->
                prefsManager.setShortcutExposureEnabled(id, true)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.navigateToShortcutSettings(context)

        // Disabling an ENABLED shortcut at the limit must work without showing a dialog
        val enabledTitle = context.getString(R.string.shortcut_dns_off_short)
        composeTestRule.safeScrollToAndAssert(enabledTitle)
        composeTestRule.onNodeWithText(enabledTitle).performClick()
        composeTestRule.waitForIdle()

        val dialogNodes = composeTestRule
            .onAllNodesWithText(context.getString(R.string.shortcut_settings_limit_reached_title))
            .fetchSemanticsNodes()
        assertTrue(
            "Limit-reached dialog must NOT appear when disabling an already-enabled shortcut",
            dialogNodes.isEmpty()
        )

        Log.i(TAG, "Disabling an enabled shortcut at the limit does not show a dialog")
    }

    // === Shortcut publication ===

    @Test
    fun shortcutSettings_enablingShortcut_shouldPublishToShortcutManager() {
        val dnsOffId = ShortcutUtils.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)
        val prefsManager = PreferencesManager.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        // Pre-condition: DNS Off disabled so enabling it is a meaningful action
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setShortcutExposureEnabled(dnsOffId, false)
        }
        composeTestRule.waitForIdle()

        val publishedIdsBefore = shortcutManager?.dynamicShortcuts?.map { it.id } ?: emptyList()
        assertFalse(
            "ShortcutManager.dynamicShortcuts must NOT contain '$dnsOffId' before enabling it. " +
                    "Published: $publishedIdsBefore",
            publishedIdsBefore.contains(dnsOffId)
        )

        composeTestRule.navigateToShortcutSettings(context)
        composeTestRule.safeScrollToAndAssert(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        // If the system limit was already reached the dialog appears — the shortcut wasn't enabled
        val dialogNodes = composeTestRule
            .onAllNodesWithText(context.getString(R.string.shortcut_settings_limit_reached_title))
            .fetchSemanticsNodes()
        if (dialogNodes.isNotEmpty()) {
            Log.i(TAG, "Limit reached; skipping publication assertion — shortcut was not enabled")
            composeTestRule.onNodeWithText(context.getString(R.string.dialog_close)).performClick()
            return
        }

        val publishedIds = shortcutManager?.dynamicShortcuts?.map { it.id } ?: emptyList()
        assertTrue(
            "ShortcutManager.dynamicShortcuts must contain '$dnsOffId' after enabling it. " +
                    "Published: $publishedIds",
            publishedIds.contains(dnsOffId)
        )
        Log.i(TAG, "Enabling DNS Off shortcut publishes '$dnsOffId' to ShortcutManager")
    }

    @Test
    fun shortcutSettings_disablingShortcut_shouldRemoveFromShortcutManager() {
        val dnsOffId = ShortcutUtils.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)
        val prefsManager = PreferencesManager.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        // Pre-condition: DNS Off enabled so disabling it removes it from ShortcutManager
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setShortcutExposureEnabled(dnsOffId, true)
        }
        composeTestRule.waitForIdle()

        val publishedIdsBefore = shortcutManager?.dynamicShortcuts?.map { it.id } ?: emptyList()
        assertTrue(
            "ShortcutManager.dynamicShortcuts must contain '$dnsOffId' before disabling it. " +
                    "Published: $publishedIdsBefore",
            publishedIdsBefore.contains(dnsOffId)
        )

        composeTestRule.navigateToShortcutSettings(context)
        composeTestRule.safeScrollToAndAssert(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        val publishedIds = shortcutManager?.dynamicShortcuts?.map { it.id } ?: emptyList()
        assertFalse(
            "ShortcutManager.dynamicShortcuts must NOT contain '$dnsOffId' after disabling it. " +
                    "Published: $publishedIds",
            publishedIds.contains(dnsOffId)
        )
        Log.i(TAG, "Disabling DNS Off shortcut removes '$dnsOffId' from ShortcutManager")
    }

    @Test
    fun shortcutSettings_enabledShortcuts_publishedInOrderMatchingGetOrderedShortcutIds() {
        val prefsManager = PreferencesManager.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        // Enable three shortcuts in a deliberately non-canonical order to verify ordering
        val targetIds = listOf(
            ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,   // built-in index 4
            ShortcutUtils.SHORTCUT_ID_DNS_OFF,      // built-in index 0
            ShortcutUtils.SHORTCUT_ID_DNS_AUTO      // built-in index 1
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val allAvailable =
                ShortcutUtils.getAvailableShortcutIds(prefsManager.dnsHostnames.value)
            allAvailable.forEach { id -> prefsManager.setShortcutExposureEnabled(id, false) }
            targetIds.forEach { id -> prefsManager.setShortcutExposureEnabled(id, true) }
        }
        composeTestRule.waitForIdle()

        // The canonical order from ShortcutUtils must be reflected in the published shortcuts
        val expectedOrder = ShortcutUtils.getOrderedShortcutIds(prefsManager.dnsHostnames.value)
            .filter { targetIds.contains(it) }

        val publishedIds = shortcutManager?.dynamicShortcuts
            ?.sortedBy { it.rank }
            ?.map { it.id } ?: emptyList()

        assertEquals(
            "Published shortcuts must follow the order defined by ShortcutUtils.getOrderedShortcutIds()",
            expectedOrder,
            publishedIds
        )
        Log.i(
            TAG,
            "Published shortcut order (by rank) matches getOrderedShortcutIds(): $publishedIds"
        )
    }

    // === Helpers ===

    /**
     * Adds enough custom DNS entries so that the total number of available shortcuts exceeds
     * [maxCount], making it possible to actually reach the limit in tests.
     *
     * Built-in shortcuts: DNS Off, Auto, AdGuard, Cloudflare, Quad9, USB On, USB Off (= 7).
     * If [maxCount] ≥ 7 we add [maxCount] − 6 custom entries so total = [maxCount] + 1.
     * The @After tearDown() removes all custom entries after each test.
     */
    private fun ensureEnoughShortcutsForLimit(maxCount: Int) {
        val builtInCount = 7
        val neededCustomCount = maxOf(0, maxCount - builtInCount + 1)
        if (neededCustomCount == 0) return

        val prefsManager = PreferencesManager.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            repeat(neededCustomCount) { i ->
                prefsManager.addCustomDnsHostname(
                    "Test DNS %02d".format(i + 1),
                    "test-dns-%02d.example.com".format(i + 1)
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Returns a map from shortcut ID to its display title as shown in [ShortcutSettingsScreen].
     * Covers all built-in shortcuts and any custom DNS entries currently in [PreferencesManager].
     */
    private fun buildIdToTitleMap(): Map<String, String> {
        val prefsManager = PreferencesManager.getInstance(context)
        return buildMap {
            put(
                ShortcutUtils.SHORTCUT_ID_DNS_OFF,
                context.getString(R.string.shortcut_dns_off_short)
            )
            put(
                ShortcutUtils.SHORTCUT_ID_DNS_AUTO,
                context.getString(R.string.shortcut_dns_auto_short)
            )
            put(
                ShortcutUtils.SHORTCUT_ID_DNS_ADGUARD,
                context.getString(R.string.shortcut_dns_adguard_short)
            )
            put(
                ShortcutUtils.SHORTCUT_ID_DNS_CLOUDFLARE,
                context.getString(R.string.shortcut_dns_cloudflare_short)
            )
            put(
                ShortcutUtils.SHORTCUT_ID_DNS_QUAD9,
                context.getString(R.string.shortcut_dns_quad9_short)
            )
            put(ShortcutUtils.SHORTCUT_ID_USB_ON, context.getString(R.string.shortcut_usb_on_short))
            put(
                ShortcutUtils.SHORTCUT_ID_USB_OFF,
                context.getString(R.string.shortcut_usb_off_short)
            )
            prefsManager.dnsHostnames.value.filter { !it.isPredefined }.forEach { entry ->
                put(ShortcutUtils.getShortcutIdForDnsEntry(entry), entry.name)
            }
        }
    }
}
