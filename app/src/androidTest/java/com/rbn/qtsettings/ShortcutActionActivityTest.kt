package com.rbn.qtsettings

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.utils.Constants
import com.rbn.qtsettings.utils.PermissionUtils
import com.rbn.qtsettings.utils.ShortcutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Instrumented tests for [ShortcutActionActivity].
 *
 * [setUp] always grants [android.Manifest.permission.WRITE_SECURE_SETTINGS] so every
 * test starts from a known, permitted state. Tests that verify the no-permission path
 * revoke the permission at the start and rely on [setUp] to re-grant it before the
 * next test. Tests that require specific developer-options state set it explicitly.
 */
@RunWith(AndroidJUnit4::class)
class ShortcutActionActivityTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefsManager = PreferencesManager.getInstance(context)
        grantWriteSecureSettings()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.dnsHostnames.value
                .filter { !it.isPredefined }
                .forEach { prefsManager.deleteCustomDnsHostname(it.id) }
            prefsManager.setAllowPinnedShortcutsWhenDisabled(false)
            ShortcutUtils.getAvailableShortcutIds(prefsManager.dnsHostnames.value)
                .forEach { prefsManager.setShortcutExposureEnabled(it, true) }
        }
    }

    // === Helpers ===

    private fun shellCommand(cmd: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(cmd)
            .use { fd ->
                ParcelFileDescriptor.AutoCloseInputStream(fd).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        while (reader.readLine() != null) { /* drain */
                        }
                    }
                }
            }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun grantWriteSecureSettings() {
        if (PermissionUtils.hasWriteSecureSettingsPermission(context)) return
        runCatching {
            shellCommand(
                "pm grant ${context.packageName} ${android.Manifest.permission.WRITE_SECURE_SETTINGS}"
            )
        }.onFailure { Log.w(TAG, "Could not grant WRITE_SECURE_SETTINGS: ${it.message}") }
    }

    private fun revokeWriteSecureSettings() {
        runCatching {
            shellCommand(
                "pm revoke ${context.packageName} ${android.Manifest.permission.WRITE_SECURE_SETTINGS}"
            )
        }.onFailure { Log.w(TAG, "Could not revoke WRITE_SECURE_SETTINGS: ${it.message}") }
    }

    private fun setDeveloperOptionsEnabled(enabled: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            Constants.DEVELOPMENT_SETTINGS_ENABLED,
            if (enabled) 1 else 0
        )
    }

    private fun launchWithAction(
        action: String,
        block: Intent.() -> Unit = {}
    ): ActivityScenario<ShortcutActionActivity> {
        val intent = Intent(context, ShortcutActionActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            block()
        }
        return ActivityScenario.launch(intent)
    }

    private fun getGlobalString(key: String): String? =
        Settings.Global.getString(context.contentResolver, key)

    private fun getGlobalInt(key: String): Int =
        Settings.Global.getInt(context.contentResolver, key, -1)

    private fun setShortcutEnabled(shortcutId: String, enabled: Boolean) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setShortcutExposureEnabled(shortcutId, enabled)
        }
    }

    private fun setAllowPinnedShortcutsWhenDisabled(enabled: Boolean) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.setAllowPinnedShortcutsWhenDisabled(enabled)
        }
    }

    // === No-permission path ===

    @Test
    fun shortcutActionActivity_withoutPermission_dnsOff_shouldNotModifySettings() {
        revokeWriteSecureSettings()

        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)
        launchWithAction(Constants.ACTION_DNS_OFF).use { }
        val dnsAfter = getGlobalString(Constants.PRIVATE_DNS_MODE)

        assertEquals(
            "DNS mode must not change without WRITE_SECURE_SETTINGS (before='$dnsBefore', after='$dnsAfter')",
            dnsBefore,
            dnsAfter
        )
        Log.i(TAG, "Without permission: DNS setting unchanged after ACTION_DNS_OFF")
    }

    @Test
    fun shortcutActionActivity_withoutPermission_usbOn_shouldNotModifySettings() {
        revokeWriteSecureSettings()

        val adbBefore = getGlobalInt(Constants.ADB_ENABLED)
        launchWithAction(Constants.ACTION_USB_ON).use { }
        val adbAfter = getGlobalInt(Constants.ADB_ENABLED)

        assertEquals(
            "ADB_ENABLED must not change without WRITE_SECURE_SETTINGS (before=$adbBefore, after=$adbAfter)",
            adbBefore,
            adbAfter
        )
        Log.i(TAG, "Without permission: USB setting unchanged after ACTION_USB_ON")
    }

    // === DNS actions ===

    @Test
    fun shortcutActionActivity_dnsOff_shouldSetPrivateDnsModeToOff() {
        // Set a known non-off starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_OFF) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_AUTO
            )
        }

        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)
        assertNotEquals(
            "DNS mode must not be 'off' before test starts (before='$dnsBefore')",
            Constants.DNS_MODE_OFF,
            dnsBefore
        )

        launchWithAction(Constants.ACTION_DNS_OFF).use { }
        val dnsAfter = getGlobalString(Constants.PRIVATE_DNS_MODE)

        assertEquals(
            "Expected PRIVATE_DNS_MODE='${Constants.DNS_MODE_OFF}', got '$dnsAfter'",
            Constants.DNS_MODE_OFF,
            dnsAfter
        )
        Log.i(TAG, "ACTION_DNS_OFF sets PRIVATE_DNS_MODE to 'off'")
    }

    @Test
    fun shortcutActionActivity_dnsAuto_shouldSetPrivateDnsModeToOpportunistic() {
        // Set a known non-auto starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_AUTO) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_OFF
            )
        }

        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)
        assertNotEquals(
            "DNS mode must not be 'auto' before test starts (before='$dnsBefore')",
            Constants.DNS_MODE_AUTO,
            dnsBefore
        )
        launchWithAction(Constants.ACTION_DNS_AUTO).use { }
        val dnsAfter = getGlobalString(Constants.PRIVATE_DNS_MODE)

        assertEquals(
            "Expected PRIVATE_DNS_MODE='${Constants.DNS_MODE_AUTO}', got '$dnsAfter'",
            Constants.DNS_MODE_AUTO,
            dnsAfter
        )
        Log.i(TAG, "ACTION_DNS_AUTO sets PRIVATE_DNS_MODE to 'opportunistic'")
    }

    @Test
    fun shortcutActionActivity_dnsAdguard_shouldSetAdguardHostname() {
        // Set a known non-on starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_ON) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_AUTO
            )
        }

        assertNotEquals(
            "Expected mode not to be '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )

        launchWithAction(Constants.ACTION_DNS_ADGUARD).use { }

        assertEquals(
            "Expected mode '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        assertEquals(
            "Expected specifier 'dns.adguard.com', got '${getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)}'",
            "dns.adguard.com",
            getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)
        )
        Log.i(TAG, "ACTION_DNS_ADGUARD sets AdGuard DNS")
    }

    @Test
    fun shortcutActionActivity_dnsCloudflare_shouldSetCloudflareHostname() {
        // Set a known non-on starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_ON) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_AUTO
            )
        }

        assertNotEquals(
            "Expected mode not to be '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )

        launchWithAction(Constants.ACTION_DNS_CLOUDFLARE).use { }

        assertEquals(
            "Expected mode '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        assertEquals(
            "Expected specifier 'one.one.one.one', got '${getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)}'",
            "one.one.one.one",
            getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)
        )
        Log.i(TAG, "ACTION_DNS_CLOUDFLARE sets Cloudflare DNS")
    }

    @Test
    fun shortcutActionActivity_dnsQuad9_shouldSetQuad9Hostname() {
        // Set a known non-on starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_ON) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_AUTO
            )
        }

        assertNotEquals(
            "Expected mode not to be '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )

        launchWithAction(Constants.ACTION_DNS_QUAD9).use { }

        assertEquals(
            "Expected mode '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        assertEquals(
            "Expected specifier 'dns.quad9.net', got '${getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)}'",
            "dns.quad9.net",
            getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)
        )
        Log.i(TAG, "ACTION_DNS_QUAD9 sets Quad9 DNS")
    }

    // === Custom DNS ===

    @Test
    fun shortcutActionActivity_dnsCustom_withKnownEntryId_shouldSetCustomDns() {
        // Set a known non-on starting state so the change is always observable.
        if (getGlobalString(Constants.PRIVATE_DNS_MODE) == Constants.DNS_MODE_ON) {
            Settings.Global.putString(
                context.contentResolver,
                Constants.PRIVATE_DNS_MODE,
                Constants.DNS_MODE_AUTO
            )
        }

        assertNotEquals(
            "Expected mode not to be '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )

        val customHostname = "dns.example.com"
        val customDisplayName = "Example DNS"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.addCustomDnsHostname(customDisplayName, customHostname)
        }
        val customEntry = prefsManager.dnsHostnames.value.first {
            !it.isPredefined && it.hostname == customHostname
        }
        setShortcutEnabled(ShortcutUtils.getShortcutIdForDnsEntry(customEntry), true)

        launchWithAction(Constants.ACTION_DNS_CUSTOM) {
            putExtra(Constants.EXTRA_DNS_ENTRY_ID, customEntry.id)
        }.use { }

        assertEquals(
            "Expected mode '${Constants.DNS_MODE_ON}', got '${getGlobalString(Constants.PRIVATE_DNS_MODE)}'",
            Constants.DNS_MODE_ON,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        assertEquals(
            "Expected specifier '$customHostname', got '${getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)}'",
            customHostname,
            getGlobalString(Constants.PRIVATE_DNS_SPECIFIER)
        )
        Log.i(TAG, "ACTION_DNS_CUSTOM with known entry ID sets custom DNS")
    }

    @Test
    fun shortcutActionActivity_dnsCustom_withMissingEntryId_shouldNotModifySettings() {
        Settings.Global.putString(
            context.contentResolver, Constants.PRIVATE_DNS_MODE, Constants.DNS_MODE_OFF
        )
        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)

        launchWithAction(Constants.ACTION_DNS_CUSTOM).use { }

        assertEquals(
            "DNS mode must not change when entry ID extra is missing",
            dnsBefore,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        Log.i(TAG, "ACTION_DNS_CUSTOM without entry ID leaves settings unchanged")
    }

    @Test
    fun shortcutActionActivity_dnsCustom_withUnknownEntryId_shouldNotModifySettings() {
        Settings.Global.putString(
            context.contentResolver, Constants.PRIVATE_DNS_MODE, Constants.DNS_MODE_OFF
        )
        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)

        launchWithAction(Constants.ACTION_DNS_CUSTOM) {
            putExtra(Constants.EXTRA_DNS_ENTRY_ID, "missing-entry-id")
        }.use { }

        assertEquals(
            "DNS mode must not change when custom entry ID is unknown",
            dnsBefore,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        Log.i(TAG, "ACTION_DNS_CUSTOM with unknown entry ID leaves settings unchanged")
    }

    @Test
    fun shortcutActionActivity_dnsCustom_whenShortcutDisabled_shouldNotModifySettings() {
        val customHostname = "dns.disabled.example.com"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            prefsManager.addCustomDnsHostname("Disabled DNS", customHostname)
        }
        val customEntry = prefsManager.dnsHostnames.value.first {
            !it.isPredefined && it.hostname == customHostname
        }
        setShortcutEnabled(ShortcutUtils.getShortcutIdForDnsEntry(customEntry), false)

        Settings.Global.putString(
            context.contentResolver, Constants.PRIVATE_DNS_MODE, Constants.DNS_MODE_OFF
        )
        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)

        launchWithAction(Constants.ACTION_DNS_CUSTOM) {
            putExtra(Constants.EXTRA_DNS_ENTRY_ID, customEntry.id)
        }.use { }

        assertEquals(
            "DNS mode must not change when the custom shortcut is disabled",
            dnsBefore,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
    }

    @Test
    fun shortcutActionActivity_dnsOff_whenShortcutDisabled_shouldNotModifySettings() {
        setShortcutEnabled(ShortcutUtils.SHORTCUT_ID_DNS_OFF, false)

        Settings.Global.putString(
            context.contentResolver,
            Constants.PRIVATE_DNS_MODE,
            Constants.DNS_MODE_AUTO
        )

        launchWithAction(Constants.ACTION_DNS_OFF).use { }

        assertEquals(
            "Disabled built-in shortcuts must not execute",
            Constants.DNS_MODE_AUTO,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
    }

    @Test
    fun shortcutActionActivity_dnsOff_whenPinnedFallbackEnabled_shouldStillExecute() {
        setShortcutEnabled(ShortcutUtils.SHORTCUT_ID_DNS_OFF, false)
        setAllowPinnedShortcutsWhenDisabled(true)

        Settings.Global.putString(
            context.contentResolver,
            Constants.PRIVATE_DNS_MODE,
            Constants.DNS_MODE_AUTO
        )

        launchWithAction(Constants.ACTION_DNS_OFF).use { }

        assertEquals(
            "Disabled shortcut should execute when pinned fallback is enabled",
            Constants.DNS_MODE_OFF,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
    }

    // === USB actions ===

    @Test
    fun shortcutActionActivity_usbOn_whenDeveloperOptionsEnabled_shouldEnableAdb() {
        setDeveloperOptionsEnabled(true)

        launchWithAction(Constants.ACTION_USB_ON).use { }
        val adbAfter = getGlobalInt(Constants.ADB_ENABLED)

        assertEquals("Expected ADB_ENABLED=1, got $adbAfter", 1, adbAfter)
        Log.i(TAG, "ACTION_USB_ON with developer options enabled sets ADB_ENABLED=1")
    }

    // NOTE: shortcutActionActivity_usbOff_whenDeveloperOptionsEnabled is intentionally absent.
    // Setting ADB_ENABLED=0 while tests run over ADB drops the transport that the test runner
    // uses, making the test run incomplete.

    @Test
    fun shortcutActionActivity_usbOn_whenDeveloperOptionsDisabled_shouldNotModifyAdb() {
        setDeveloperOptionsEnabled(false)

        val adbBefore = getGlobalInt(Constants.ADB_ENABLED)
        launchWithAction(Constants.ACTION_USB_ON).use { }
        val adbAfter = getGlobalInt(Constants.ADB_ENABLED)

        setDeveloperOptionsEnabled(true) // restore

        assertEquals(
            "ADB_ENABLED must not change when developer options disabled (before=$adbBefore, after=$adbAfter)",
            adbBefore,
            adbAfter
        )
        Log.i(TAG, "ACTION_USB_ON with developer options disabled leaves ADB_ENABLED unchanged")
    }

    @Test
    fun shortcutActionActivity_usbOff_whenDeveloperOptionsDisabled_shouldNotModifyAdb() {
        setDeveloperOptionsEnabled(false)

        val adbBefore = getGlobalInt(Constants.ADB_ENABLED)
        launchWithAction(Constants.ACTION_USB_OFF).use { }
        val adbAfter = getGlobalInt(Constants.ADB_ENABLED)

        setDeveloperOptionsEnabled(true) // restore

        assertEquals(
            "ADB_ENABLED must not change when developer options disabled (before=$adbBefore, after=$adbAfter)",
            adbBefore,
            adbAfter
        )
        Log.i(TAG, "ACTION_USB_OFF with developer options disabled leaves ADB_ENABLED unchanged")
    }

    // === Edge cases ===

    @Test
    fun shortcutActionActivity_withNullAction_shouldNotCrash() {
        val intent = Intent(context, ShortcutActionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // ActivityScenario.launch throws if the activity crashes
        ActivityScenario.launch<ShortcutActionActivity>(intent).use { }
        Log.i(TAG, "Activity handles null action without crashing")
    }

    @Test
    fun shortcutActionActivity_withUnknownAction_shouldNotModifySettings() {
        val dnsBefore = getGlobalString(Constants.PRIVATE_DNS_MODE)

        launchWithAction("com.rbn.qtsettings.action.UNKNOWN_ACTION").use { }

        assertEquals(
            "Settings must not change for an unrecognised action",
            dnsBefore,
            getGlobalString(Constants.PRIVATE_DNS_MODE)
        )
        Log.i(TAG, "Unknown action leaves system settings unchanged")
    }

    companion object {
        private const val TAG = "ShortcutActionActivityTest"
    }
}
