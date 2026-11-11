package com.rbn.qtsettings.feature.dns.domain

import com.rbn.qtsettings.core.revert.RevertCountdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** Owns the restoration ticket and its countdown; one instance serves the tile and the app. */
class DnsAutoRevert(private val actions: DnsActions, scope: CoroutineScope) {
    private data class Pending(val previous: DnsTarget, val generation: Long)

    private var pending: Pending? = null
    private val countdown = RevertCountdown(scope)

    /** Seconds left, or null while nothing is pending. */
    val remainingSeconds: StateFlow<Int?> = countdown.remainingSeconds
    val previous: DnsTarget? get() = pending?.previous

    /** Arms the ticket without a countdown; callers supply their own ticks. */
    fun schedule(previous: DnsTarget, generation: Long) {
        pending = Pending(previous, generation)
    }

    /** Arms the ticket and restores it after [delaySeconds] unless a direct action intervenes. */
    fun schedule(previous: DnsTarget, generation: Long, delaySeconds: Int, onFinished: (DnsActionResult?, DnsTarget) -> Unit) {
        schedule(previous, generation)
        countdown.start(delaySeconds, ::isCurrent) { onFinished(finish(), previous) }
    }

    fun isCurrent() = pending?.generation == actions.generation()

    /** Returns whether a countdown was pending, so a silent cancel stays silent. */
    fun cancel(): Boolean {
        pending = null
        return countdown.cancel()
    }

    fun finish(): DnsActionResult? {
        val ticket = pending ?: return null
        pending = null
        return actions.restore(ticket.previous, ticket.generation)
    }
}
