package com.rbn.qtsettings.feature.usb.domain

/** Timer-independent owner. Android supplies ticks and cancels it on service destruction. */
class UsbAutoRevert(private val actions: UsbActions) {
    private data class Pending(val previous: UsbSystemState, val generation: Long)
    private var pending: Pending? = null
    val previous: UsbSystemState? get() = pending?.previous
    fun schedule(previous: UsbSystemState, generation: Long) { pending = Pending(previous, generation) }
    fun isCurrent(): Boolean = pending?.generation == actions.generation()
    fun cancel() { pending = null }
    fun finish(settings: UsbSettings): UsbActionResult? {
        val ticket = pending ?: return null
        pending = null
        return actions.restore(ticket.previous, ticket.generation, settings)
    }
}
