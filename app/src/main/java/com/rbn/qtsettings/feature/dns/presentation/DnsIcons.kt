package com.rbn.qtsettings.feature.dns.presentation

import androidx.annotation.DrawableRes
import com.rbn.qtsettings.R
import com.rbn.qtsettings.feature.dns.domain.DnsTarget

/** One source for the per-state DNS glyphs the tile, the shortcuts and the overview all show. */
@DrawableRes
fun dnsIconForHostname(hostname: String?): Int = when (hostname) {
    "dns.adguard.com" -> R.drawable.ic_dns_on_adguard
    "one.one.one.one" -> R.drawable.ic_dns_on_cloudflare
    "dns.quad9.net" -> R.drawable.ic_dns_on_quad9_security
    else -> R.drawable.ic_dns_on
}

/** Null means the system value could not be read, which has no state glyph of its own. */
@DrawableRes
fun dnsIconForTarget(target: DnsTarget?): Int? = when (target) {
    DnsTarget.Off -> R.drawable.ic_dns_off
    DnsTarget.Automatic -> R.drawable.ic_dns_auto
    is DnsTarget.Hostname -> dnsIconForHostname(target.value)
    null -> null
}
