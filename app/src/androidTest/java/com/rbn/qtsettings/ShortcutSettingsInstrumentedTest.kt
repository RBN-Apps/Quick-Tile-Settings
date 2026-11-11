package com.rbn.qtsettings

import android.content.Context
import android.content.pm.ShortcutManager
import android.util.Log
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.app.AppStores
import com.rbn.qtsettings.feature.shortcuts.domain.ShortcutCatalog
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
        val prefsManager = AppStores.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.dns.dnsHostnames.value
                .filter { !it.isPredefined }
                .forEach { prefsManager.dns.deleteCustomDnsHostname(it.id) }
            val allAvailableIds =
                ShortcutCatalog.getAvailableShortcutIds(prefsManager.dns.dnsHostnames.value)
            prefsManager.shortcuts.setAllowPinnedShortcutsWhenDisabled(false)
            allAvailableIds.forEach { prefsManager.shortcuts.setShortcutExposureEnabled(it, false) }
            allAvailableIds.forEach { prefsManager.shortcuts.setShortcutFavorite(it, false) }
        }
    }

    @After
    fun tearDown() {
        // Remove any custom DNS entries added during limit tests so they don't leak into other tests
        val prefsManager = AppStores.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.dns.dnsHostnames.value
                .filter { !it.isPredefined }
                .forEach { prefsManager.dns.deleteCustomDnsHostname(it.id) }
            val allAvailableIds =
                ShortcutCatalog.getAvailableShortcutIds(prefsManager.dns.dnsHostnames.value)
            prefsManager.shortcuts.setAllowPinnedShortcutsWhenDisabled(false)
            allAvailableIds.forEach { prefsManager.shortcuts.setShortcutExposureEnabled(it, false) }
            allAvailableIds.forEach { prefsManager.shortcuts.setShortcutFavorite(it, false) }
        }
    }

    private fun scrollToShortcut(text: String) {
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        composeTestRule.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pinnedWarning_survivesRecreation_cancelDoesNotEnableFallback() {
        composeTestRule.navigateToShortcutSettings(context)
        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_allow_pinned_when_disabled))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_allow_pinned_warning_title))
            .assertIsDisplayed()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_allow_pinned_warning_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.dialog_cancel)).performClick()
        assertFalse(AppStores.getInstance(context).shortcuts.state.value.allowPinnedWhenDisabled)
        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_title)).assertIsDisplayed()
    }

    // === Screen content ===

    @Test
    fun overview_listsBothGroupsWithEveryBuiltInShortcut_andTheActiveCount() {
        composeTestRule.navigateToShortcutSettings(context)

        val configuration = AppStores.getInstance(context).shortcuts.state.value
        composeTestRule.onNodeWithText(context.getString(R.string.shortcut_settings_limit,
            configuration.enabledIds.size, configuration.maxCount)).assertIsDisplayed()
        listOf(
            R.string.setting_title_private_dns,
            R.string.shortcut_dns_off_short,
            R.string.shortcut_dns_auto_short,
            R.string.shortcut_dns_adguard_short,
            R.string.shortcut_dns_cloudflare_short,
            R.string.shortcut_dns_quad9_short,
            R.string.setting_title_usb_debugging,
            R.string.shortcut_usb_on_short,
            R.string.shortcut_usb_off_short
        ).forEach { scrollToShortcut(context.getString(it)) }
    }

    // === Toggle interaction ===
    // setUp() disables every shortcut, so enabling one can never hit the limit here.

    @Test
    fun enablingShortcut_increasesActiveCount_andPublishesItToShortcutManager() {
        val dnsOffId = ShortcutCatalog.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)
        val prefsManager = AppStores.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val countBefore = prefsManager.shortcuts.enabledShortcutIds.value.size
        assertFalse(shortcutManager.dynamicShortcuts.any { it.id == dnsOffId })

        composeTestRule.navigateToShortcutSettings(context)
        scrollToShortcut(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        assertEquals(countBefore + 1, prefsManager.shortcuts.enabledShortcutIds.value.size)
        val publishedIds = shortcutManager.dynamicShortcuts.map { it.id }
        assertTrue("'$dnsOffId' must be published, got $publishedIds", publishedIds.contains(dnsOffId))
    }

    @Test
    fun disablingShortcut_decreasesActiveCount_andRemovesItFromShortcutManager() {
        val dnsOffId = ShortcutCatalog.SHORTCUT_ID_DNS_OFF
        val dnsOffTitle = context.getString(R.string.shortcut_dns_off_short)
        val prefsManager = AppStores.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.shortcuts.setShortcutExposureEnabled(dnsOffId, true)
        }
        composeTestRule.waitForIdle()
        val countBefore = prefsManager.shortcuts.enabledShortcutIds.value.size
        assertTrue(shortcutManager.dynamicShortcuts.any { it.id == dnsOffId })

        composeTestRule.navigateToShortcutSettings(context)
        scrollToShortcut(dnsOffTitle)
        composeTestRule.onNodeWithText(dnsOffTitle).performClick()
        composeTestRule.waitForIdle()

        assertEquals(countBefore - 1, prefsManager.shortcuts.enabledShortcutIds.value.size)
        val publishedIds = shortcutManager.dynamicShortcuts.map { it.id }
        assertFalse("'$dnsOffId' must be removed, got $publishedIds", publishedIds.contains(dnsOffId))
    }

    // === Limit-reached dialog ===

    @Test
    fun shortcutSettings_limitReachedDialog_shouldDisplayAndDismiss() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val maxCount = shortcutManager?.maxShortcutCountPerActivity ?: 5

        // Ensure there are more shortcuts available than the system limit so the limit can be hit
        ensureEnoughShortcutsForLimit(maxCount)

        val prefsManager = AppStores.getInstance(context)
        val allOrderedIds = ShortcutCatalog.getOrderedShortcutIds(prefsManager.dns.dnsHostnames.value)
        val idToTitle = buildIdToTitleMap()

        check(allOrderedIds.size > maxCount) {
            "Expected more than $maxCount shortcuts after adding custom entries, " +
                    "but got ${allOrderedIds.size}"
        }

        // Set up known state: disable all, then enable exactly maxCount shortcuts
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            allOrderedIds.forEach { id -> prefsManager.shortcuts.setShortcutExposureEnabled(id, false) }
            allOrderedIds.take(maxCount).forEach { id ->
                prefsManager.shortcuts.setShortcutExposureEnabled(id, true)
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

        scrollToShortcut(disabledTitle)
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

        val prefsManager = AppStores.getInstance(context)
        val allOrderedIds = ShortcutCatalog.getOrderedShortcutIds(prefsManager.dns.dnsHostnames.value)

        check(allOrderedIds.size > maxCount) {
            "Expected more than $maxCount shortcuts after adding custom entries, " +
                    "but got ${allOrderedIds.size}"
        }

        // Fill shortcuts to the limit; DNS Off (index 0) is always among the enabled ones
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            allOrderedIds.forEach { id -> prefsManager.shortcuts.setShortcutExposureEnabled(id, false) }
            allOrderedIds.take(maxCount).forEach { id ->
                prefsManager.shortcuts.setShortcutExposureEnabled(id, true)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.navigateToShortcutSettings(context)

        // Disabling an ENABLED shortcut at the limit must work without showing a dialog
        val enabledTitle = context.getString(R.string.shortcut_dns_off_short)
        scrollToShortcut(enabledTitle)
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
    fun shortcutSettings_enabledShortcuts_publishedInOrderMatchingGetOrderedShortcutIds() {
        val prefsManager = AppStores.getInstance(context)
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        // Enable three shortcuts in a deliberately non-canonical order to verify ordering
        val targetIds = listOf(
            ShortcutCatalog.SHORTCUT_ID_DNS_QUAD9,   // built-in index 4
            ShortcutCatalog.SHORTCUT_ID_DNS_OFF,      // built-in index 0
            ShortcutCatalog.SHORTCUT_ID_DNS_AUTO      // built-in index 1
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val allAvailable =
                ShortcutCatalog.getAvailableShortcutIds(prefsManager.dns.dnsHostnames.value)
            allAvailable.forEach { id -> prefsManager.shortcuts.setShortcutExposureEnabled(id, false) }
            targetIds.forEach { id -> prefsManager.shortcuts.setShortcutExposureEnabled(id, true) }
        }
        composeTestRule.waitForIdle()

        // The canonical order from ShortcutCatalog must be reflected in the published shortcuts
        val expectedOrder = ShortcutCatalog.getOrderedShortcutIds(prefsManager.dns.dnsHostnames.value)
            .filter { targetIds.contains(it) }

        val publishedIds = shortcutManager?.dynamicShortcuts
            ?.sortedBy { it.rank }
            ?.map { it.id } ?: emptyList()

        assertEquals(
            "Published shortcuts must follow the order defined by ShortcutCatalog.getOrderedShortcutIds()",
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

        val prefsManager = AppStores.getInstance(context)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            repeat(neededCustomCount) { i ->
                prefsManager.dns.addCustomDnsHostname(
                    "Test DNS %02d".format(i + 1),
                    "test-dns-%02d.example.com".format(i + 1)
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Returns a map from shortcut ID to its display title as shown in [ShortcutSettingsScreen].
     * Covers all built-in shortcuts and any custom DNS entries currently in [AppStores].
     */
    private fun buildIdToTitleMap(): Map<String, String> {
        val prefsManager = AppStores.getInstance(context)
        return buildMap {
            put(
                ShortcutCatalog.SHORTCUT_ID_DNS_OFF,
                context.getString(R.string.shortcut_dns_off_short)
            )
            put(
                ShortcutCatalog.SHORTCUT_ID_DNS_AUTO,
                context.getString(R.string.shortcut_dns_auto_short)
            )
            put(
                ShortcutCatalog.SHORTCUT_ID_DNS_ADGUARD,
                context.getString(R.string.shortcut_dns_adguard_short)
            )
            put(
                ShortcutCatalog.SHORTCUT_ID_DNS_CLOUDFLARE,
                context.getString(R.string.shortcut_dns_cloudflare_short)
            )
            put(
                ShortcutCatalog.SHORTCUT_ID_DNS_QUAD9,
                context.getString(R.string.shortcut_dns_quad9_short)
            )
            put(ShortcutCatalog.SHORTCUT_ID_USB_ON, context.getString(R.string.shortcut_usb_on_short))
            put(
                ShortcutCatalog.SHORTCUT_ID_USB_OFF,
                context.getString(R.string.shortcut_usb_off_short)
            )
            prefsManager.dns.dnsHostnames.value.filter { !it.isPredefined }.forEach { entry ->
                put(ShortcutCatalog.getShortcutIdForDnsEntry(entry), entry.name)
            }
        }
    }
}
