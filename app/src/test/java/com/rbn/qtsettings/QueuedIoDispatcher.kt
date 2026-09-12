package com.rbn.qtsettings

import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import org.robolectric.Shadows.shadowOf

/** Controls IO completion while keeping the project's existing Robolectric main dispatcher. */
internal class QueuedIoDispatcher : CoroutineDispatcher() {
    private val pending = ArrayDeque<Runnable>()
    override fun dispatch(context: CoroutineContext, block: Runnable) { pending.addLast(block) }
    fun drain() {
        do {
            while (pending.isNotEmpty()) pending.removeFirst().run()
            shadowOf(Looper.getMainLooper()).idle()
        } while (pending.isNotEmpty())
    }
}
