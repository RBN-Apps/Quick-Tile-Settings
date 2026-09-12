package com.rbn.qtsettings.feature.automation

import android.Manifest
import android.content.Context
import android.provider.Settings
import com.rbn.qtsettings.data.DetectedNetworkState
import com.rbn.qtsettings.feature.automation.data.AutomationSettingsStore
import com.rbn.qtsettings.feature.automation.domain.VpnAutomation
import com.rbn.qtsettings.feature.dns.domain.DnsActionResult
import com.rbn.qtsettings.feature.dns.domain.DnsActions
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.platform.automation.AndroidVpnAutomationGateway
import com.rbn.qtsettings.platform.dns.AndroidDnsSystemGateway
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.NETWORK_TYPE_WIFI
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.NetworkDnsAutomation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutomationTimerCompatibilityTest {
    @Test
    fun automationPreservesPendingTimer_butExplicitDnsActionInvalidatesIt() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        val store = AutomationSettingsStore(context.getSharedPreferences("qt_settings_prefs", Context.MODE_PRIVATE))
        val actions = DnsActions(AndroidDnsSystemGateway(context))
        val vpn = VpnAutomation(AndroidVpnAutomationGateway(context))
        actions.set(DnsTarget.Hostname("manual.example.com"))
        val pendingGeneration = actions.generation()

        store.setDnsStateOnWifi(DNS_MODE_AUTO)
        assertTrue(NetworkDnsAutomation.apply(context, store, DetectedNetworkState(NETWORK_TYPE_WIFI)))
        assertEquals(pendingGeneration, actions.generation())
        vpn.connected()
        assertEquals(DNS_MODE_OFF, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
        assertEquals(pendingGeneration, actions.generation())
        assertTrue(vpn.disconnected({ false }))
        assertEquals(pendingGeneration, actions.generation())

        // Preserve the established contract: automation does not cancel an already pending revert.
        assertEquals(DnsActionResult.Success, actions.restore(DnsTarget.Off, pendingGeneration))
        val newerGeneration = actions.generation()
        actions.set(DnsTarget.Automatic)
        assertNull(actions.restore(DnsTarget.Off, newerGeneration))
        assertEquals(DNS_MODE_AUTO, Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE))
    }
}
