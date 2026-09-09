package com.rbn.qtsettings

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

/** Injects real touchscreen events through SystemUI's edge recognizer; never calls a back callback. */
internal class SystemBackGesture(private val width: Int, height: Int, private val right: Boolean) {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    // Start above the IME: the keyboard may exclude its own edge touch region.
    private val y = height * .3f
    private val startX = if (right) width - 2f else 2f
    private var x = startX
    private var downTime = 0L

    fun begin() {
        downTime = SystemClock.uptimeMillis()
        send(MotionEvent.ACTION_DOWN)
        moveTo(.32f)
    }

    fun commit() {
        moveTo(.65f)
        send(MotionEvent.ACTION_UP)
    }

    fun cancel() {
        // Retrace towards the originating edge and release, as a user cancelling the preview.
        moveTo(.005f)
        send(MotionEvent.ACTION_UP)
    }

    private fun moveTo(fraction: Float) {
        val from = x
        val target = startX + (if (right) -1 else 1) * width * fraction
        repeat(16) { step ->
            SystemClock.sleep(16)
            x = from + (target - from) * (step + 1) / 16f
            send(MotionEvent.ACTION_MOVE)
        }
    }

    private fun send(action: Int) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try { check(automation.injectInputEvent(event, true)) { "Touch injection failed" } }
        finally { event.recycle() }
    }
}
