package com.rbn.qtsettings.platform.dns

import android.content.Context
import android.provider.Settings
import com.rbn.qtsettings.data.PrivateDnsTarget
import com.rbn.qtsettings.feature.dns.data.toDomain
import com.rbn.qtsettings.feature.dns.data.toPersisted
import com.rbn.qtsettings.feature.dns.domain.DnsSystemGateway
import com.rbn.qtsettings.feature.dns.domain.DnsTarget
import com.rbn.qtsettings.utils.AutoRevertCoordinator
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_MODE
import com.rbn.qtsettings.utils.Constants.PRIVATE_DNS_SPECIFIER
import com.rbn.qtsettings.utils.PermissionUtils

class AndroidDnsSystemGateway(context: Context) : DnsSystemGateway {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    override fun hasPermission() = PermissionUtils.hasWriteSecureSettingsPermission(context)
    override fun read(): DnsTarget? {
        val mode = Settings.Global.getString(resolver, PRIVATE_DNS_MODE) ?: return null
        return PrivateDnsTarget(mode, Settings.Global.getString(resolver, PRIVATE_DNS_SPECIFIER)).toDomain()
            ?.takeUnless { it is DnsTarget.Hostname && it.value.isBlank() }
    }
    override fun apply(target: DnsTarget): Boolean {
        val previousMode = Settings.Global.getString(resolver, PRIVATE_DNS_MODE)
        val previousHost = Settings.Global.getString(resolver, PRIVATE_DNS_SPECIFIER)
        val persisted = target.toPersisted()
        val success = runCatching {
            (target !is DnsTarget.Hostname || Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, persisted.hostname)) &&
                Settings.Global.putString(resolver, PRIVATE_DNS_MODE, persisted.mode)
        }.getOrDefault(false)
        if (!success) {
            val hostRestored = runCatching { Settings.Global.putString(resolver, PRIVATE_DNS_SPECIFIER, previousHost) }.getOrDefault(false)
            val modeRestored = runCatching { Settings.Global.putString(resolver, PRIVATE_DNS_MODE, previousMode) }.getOrDefault(false)
            if (!hostRestored || !modeRestored) invalidateRevert()
        }
        return success
    }
    override fun generation() = AutoRevertCoordinator.dnsGeneration(context)
    override fun invalidateRevert() = AutoRevertCoordinator.invalidateDnsAutoRevert(context)
}
