package com.rbn.qtsettings.feature.dns.domain

/** Owns the restoration ticket; the tile supplies Android timer ticks. */
class DnsAutoRevert(private val actions: DnsActions) {
    private data class Pending(val previous: DnsTarget, val generation: Long)
    private var pending: Pending? = null
    fun schedule(previous: DnsTarget, generation: Long) { pending = Pending(previous, generation) }
    fun isCurrent() = pending?.generation == actions.generation()
    fun cancel() { pending = null }
    fun finish(): DnsActionResult? {
        val ticket = pending ?: return null
        pending = null
        return actions.restore(ticket.previous, ticket.generation)
    }
}
