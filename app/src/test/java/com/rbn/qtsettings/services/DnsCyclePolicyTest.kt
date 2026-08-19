package com.rbn.qtsettings.services

import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.utils.Constants.DNS_MODE_AUTO
import com.rbn.qtsettings.utils.Constants.DNS_MODE_OFF
import com.rbn.qtsettings.utils.Constants.DNS_MODE_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DnsCyclePolicyTest {
    private val adGuard = DnsHostnameEntry(name = "AdGuard", hostname = "dns.adguard.com")
    private val quad9 = DnsHostnameEntry(name = "Quad9", hostname = "dns.quad9.net")

    @Test
    fun `returns null when no states are enabled`() {
        assertNull(
            DnsCyclePolicy.nextState(
                currentMode = DNS_MODE_OFF,
                currentHostname = null,
                includeOff = false,
                includeAuto = false,
                hostnames = emptyList()
            )
        )
    }

    @Test
    fun `cycles through off auto and selected hostnames in order`() {
        val next = DnsCyclePolicy.nextState(
            currentMode = DNS_MODE_AUTO,
            currentHostname = null,
            includeOff = true,
            includeAuto = true,
            hostnames = listOf(adGuard, quad9)
        )

        assertEquals(DnsCycleState(DNS_MODE_ON, adGuard.hostname), next)
    }

    @Test
    fun `wraps from last hostname to off`() {
        val next = DnsCyclePolicy.nextState(
            currentMode = DNS_MODE_ON,
            currentHostname = quad9.hostname,
            includeOff = true,
            includeAuto = true,
            hostnames = listOf(adGuard, quad9)
        )

        assertEquals(DnsCycleState(DNS_MODE_OFF), next)
    }

    @Test
    fun `starts with first enabled state when current state is unavailable`() {
        val next = DnsCyclePolicy.nextState(
            currentMode = DNS_MODE_ON,
            currentHostname = "unknown.example",
            includeOff = false,
            includeAuto = true,
            hostnames = listOf(adGuard)
        )

        assertEquals(DnsCycleState(DNS_MODE_AUTO), next)
    }
}
