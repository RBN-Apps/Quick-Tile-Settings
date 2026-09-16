package com.rbn.qtsettings.core.revert

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Replaces the per-service CountDownTimer that died with its tile service. Living at process
 * scope lets a pending revert outlive the quick settings panel and lets the app arm and observe
 * the very same one.
 */
class RevertCountdown(private val scope: CoroutineScope) {
    private val mutableRemainingSeconds = MutableStateFlow<Int?>(null)

    /** Seconds left, or null while nothing is pending. */
    val remainingSeconds: StateFlow<Int?> = mutableRemainingSeconds.asStateFlow()

    private var job: Job? = null

    /** [isCurrent] is re-checked every tick so a later direct action stops the countdown. */
    fun start(seconds: Int, isCurrent: () -> Boolean, onFinish: () -> Unit) {
        job?.cancel()
        mutableRemainingSeconds.value = seconds
        job = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                // A direct action elsewhere stole the ticket: stop early but still finish, so
                // the owner gets to drop its bookkeeping exactly as it would after a restore.
                if (!isCurrent()) break
                remaining -= 1
                mutableRemainingSeconds.value = remaining
            }
            // Restore first: collectors refresh their view on the null emission and must not
            // read the pre-revert system state.
            try {
                onFinish()
            } finally {
                mutableRemainingSeconds.value = null
                job = null
            }
        }
    }

    /** Returns whether a countdown was actually pending, so callers can stay quiet otherwise. */
    fun cancel(): Boolean {
        val pending = job != null
        job?.cancel()
        job = null
        mutableRemainingSeconds.value = null
        return pending
    }
}
