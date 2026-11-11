package com.rbn.qtsettings

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

/** Injects real touchscreen events through SystemUI's edge recognizer; never calls a back callback. */
internal class SystemBackGesture(private val width: Int, height: Int, private val right: Boolean) : AutoCloseable {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    // Start above the IME: the keyboard may exclude its own edge touch region.
    private val y = height * .3f
    private val startX = if (right) width - 2f else 2f
    private var x = startX
    private var downTime = 0L
    private var eventTime = 0L
    private var pressed = false

    fun begin() {
        check(!pressed)
        downTime = SystemClock.uptimeMillis()
        eventTime = downTime
        pressed = true
        send(MotionEvent.ACTION_DOWN)
        moveTo(.32f)
    }

    fun commit() {
        moveTo(.65f)
        release(MotionEvent.ACTION_UP)
    }

    fun cancel() {
        // Retrace towards the originating edge and release, as a user cancelling the preview.
        moveTo(.005f)
        release(MotionEvent.ACTION_UP)
    }

    private fun moveTo(fraction: Float) {
        val from = x
        val target = startX + (if (right) -1 else 1) * width * fraction
        repeat(16) { step ->
            // Preserve a real event-time cadence independent of Binder/render latency.
            eventTime += 16
            SystemClock.sleep(16)
            x = from + (target - from) * (step + 1) / 16f
            send(MotionEvent.ACTION_MOVE)
        }
    }

    override fun close() {
        if (pressed) release(MotionEvent.ACTION_CANCEL)
    }

    private fun release(action: Int) {
        // Flush all previously queued moves before returning to the assertion or next gesture.
        try { send(action, synchronous = true) } finally { pressed = false }
    }

    private fun send(action: Int, synchronous: Boolean = false) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        // Queue at the cadence above. Waiting for every MOVE to finish rendering can stretch one
        // gesture over several seconds on SwiftShader and make SystemUI time it out.
        try { check(automation.injectInputEvent(event, synchronous)) { "Touch injection failed" } }
        finally { event.recycle() }
    }
}
