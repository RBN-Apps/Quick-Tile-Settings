package com.rbn.qtsettings.utils

import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.data.WifiDnsRule
import com.rbn.qtsettings.data.WifiNetworkIdentity
import com.rbn.qtsettings.feature.automation.data.automationTarget
import com.rbn.qtsettings.feature.automation.data.toAutomationIdentity
import com.rbn.qtsettings.feature.automation.data.toAutomationRule
import com.rbn.qtsettings.feature.automation.domain.AutomationPolicy
import com.rbn.qtsettings.feature.dns.data.toPersisted

/** Old test shape exercising the single production policy. */
fun WifiNetworkRuleUtils.resolveDnsTarget(
        rulesEnabled: Boolean,
        rules: List<WifiDnsRule>,
        currentNetwork: WifiNetworkIdentity?,
        defaultTarget: PrivateDnsTarget
    ): PrivateDnsTarget? {
        return AutomationPolicy.wifiTarget(
            rulesEnabled = rulesEnabled,
            rules = rules.mapNotNull { it.toAutomationRule() },
            identity = currentNetwork?.toAutomationIdentity(),
            defaultTarget = automationTarget(defaultTarget.mode, defaultTarget.hostname)
        )?.toPersisted()
    }
