package com.rbn.qtsettings.feature.dns.domain

import com.rbn.qtsettings.utils.DnsHostnameValidator

interface DnsSystemGateway {
    fun hasPermission(): Boolean
    fun read(): DnsTarget?
    /** Applies a target atomically where possible, rolling back failed hostname writes. */
    fun apply(target: DnsTarget): Boolean
    fun generation(): Long
    fun invalidateRevert()
}

enum class DnsActionResult { Success, PermissionMissing, InvalidHostname, NoCycleStates, Failed }

/** The same serialized action path serves UI, tile, shortcuts and pending restoration. */
class DnsActions(private val gateway: DnsSystemGateway) {
    fun read() = runCatching { gateway.read() }.getOrNull()
    fun hasPermission() = gateway.hasPermission()
    fun generation() = gateway.generation()
    fun set(target: DnsTarget): DnsActionResult = synchronized(lock) {
        if (!gateway.hasPermission()) return@synchronized DnsActionResult.PermissionMissing
        val normalized = if (target is DnsTarget.Hostname) {
            if (!DnsHostnameValidator.isValid(target.value)) return@synchronized DnsActionResult.InvalidHostname
            DnsTarget.Hostname(target.value.trim())
        } else target
        if (runCatching { gateway.apply(normalized) }.getOrDefault(false)) {
            gateway.invalidateRevert()
            DnsActionResult.Success
        } else DnsActionResult.Failed
    }
    data class CycleResult(val result: DnsActionResult, val previous: DnsTarget? = null, val generation: Long = 0)
    fun cycle(settings: DnsSettings): CycleResult = synchronized(lock) {
        val before = runCatching { gateway.read() }.getOrElse {
            return@synchronized CycleResult(DnsActionResult.Failed)
        } ?: DnsTarget.Off
        val target = nextDnsTarget(before, settings)
            ?: return@synchronized CycleResult(DnsActionResult.NoCycleStates)
        CycleResult(set(target), before, generation())
    }
    fun restore(previous: DnsTarget, expectedGeneration: Long): DnsActionResult? = synchronized(lock) {
        if (generation() != expectedGeneration) null else set(previous)
    }
    private companion object { val lock = Any() }
}
