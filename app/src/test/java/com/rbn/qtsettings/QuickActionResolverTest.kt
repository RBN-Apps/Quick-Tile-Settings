package com.rbn.qtsettings

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rbn.qtsettings.data.PreferencesManager
import com.rbn.qtsettings.services.PrivateDnsTileService
import com.rbn.qtsettings.services.UsbDebuggingTileService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class QuickActionResolverTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        resetPreferencesManagerSingleton()
        clearPreferences()
        prefsManager = PreferencesManager.getInstance(context)
    }

    @After
    fun tearDown() {
        clearPreferences()
        resetPreferencesManagerSingleton()
    }

    @Test
    fun resolveTileType_privateDnsComponent_returnsDns() {
        val componentName = ComponentName(context, PrivateDnsTileService::class.java)

        val tileType = QuickActionResolver.resolveTileType(componentName, null)

        assertEquals(TileType.DNS, tileType)
    }

    @Test
    fun resolveTileType_usbComponent_returnsUsb() {
        val componentName = ComponentName(context, UsbDebuggingTileService::class.java)

        val tileType = QuickActionResolver.resolveTileType(componentName, null)

        assertEquals(TileType.USB, tileType)
    }

    @Test
    fun resolveTileType_flattenedPrivateDnsComponent_returnsDns() {
        val componentNameText = ComponentName(context, PrivateDnsTileService::class.java)
            .flattenToString()

        val tileType = QuickActionResolver.resolveTileType(null, componentNameText)

        assertEquals(TileType.DNS, tileType)
    }

    @Test
    fun resolveTileType_unknownComponent_returnsAllFallback() {
        val tileType = QuickActionResolver.resolveTileType(null, "unknown")

        assertEquals(TileType.ALL, tileType)
    }

    @Test
    fun quickActionDialog_withoutPermission_opensAppForTileConfiguration() {
        val application = context as Application
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        val tileServiceName = PrivateDnsTileService::class.java.name
        val launchIntent = Intent(application, QuickActionDialogActivity::class.java).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, tileServiceName)
        }

        val activity = Robolectric.buildActivity(QuickActionDialogActivity::class.java, launchIntent)
            .create()
            .get()
        val openedIntent = shadowOf(activity).nextStartedActivity

        assertEquals(ComponentName(application, MainActivity::class.java), openedIntent.component)
        assertEquals(tileServiceName, openedIntent.getStringExtra(Intent.EXTRA_COMPONENT_NAME))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun buildQuickActions_dnsTile_returnsOnlyConfiguredDnsActions() {
        prefsManager.setDnsToggleAuto(false)
        val cloudflare = prefsManager.dnsHostnames.value.first { it.id == "cloudflare_default" }
        prefsManager.updateDnsHostnameEntry(cloudflare.copy(isSelectedForCycle = false))

        val actions = QuickActionResolver.buildQuickActions(context, prefsManager, TileType.DNS)

        assertTrue(actions.any { it.kind == QuickActionKind.DnsOff })
        assertFalse(actions.any { it.kind == QuickActionKind.DnsAuto })
        assertTrue(
            actions.any {
                (it.kind as? QuickActionKind.DnsHostname)?.entry?.hostname == "dns.adguard.com"
            }
        )
        assertFalse(
            actions.any {
                (it.kind as? QuickActionKind.DnsHostname)?.entry?.hostname == "one.one.one.one"
            }
        )
        assertFalse(actions.any { it.kind is QuickActionKind.UsbDebugging })
    }

    @Test
    fun buildQuickActions_usbTile_returnsOnlyConfiguredUsbActions() {
        prefsManager.setUsbToggleEnable(false)
        prefsManager.setUsbToggleDisable(true)

        val actions = QuickActionResolver.buildQuickActions(context, prefsManager, TileType.USB)

        assertFalse(actions.any { it.kind == QuickActionKind.DnsOff })
        assertFalse(actions.any { it.kind == QuickActionKind.DnsAuto })
        assertFalse(actions.any { it.kind is QuickActionKind.DnsHostname })
        assertFalse(actions.any { (it.kind as? QuickActionKind.UsbDebugging)?.enable == true })
        assertTrue(actions.any { (it.kind as? QuickActionKind.UsbDebugging)?.enable == false })
    }

    @Test
    fun buildQuickActions_allFallback_includesDnsAndUsbActions() {
        val actions = QuickActionResolver.buildQuickActions(context, prefsManager, TileType.ALL)

        assertTrue(actions.any { it.kind == QuickActionKind.DnsOff })
        assertTrue(actions.any { it.kind == QuickActionKind.DnsAuto })
        assertTrue(actions.any { it.kind is QuickActionKind.DnsHostname })
        assertTrue(actions.any { it.kind is QuickActionKind.UsbDebugging })
    }

    private fun clearPreferences() {
        context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun resetPreferencesManagerSingleton() {
        val instanceField = PreferencesManager::class.java.getDeclaredField("INSTANCE")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }
}
