package com.rbn.qtsettings

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

fun ComposeTestRule.waitForDialogWindowFocus(): View = waitForWindowFocus(onNode(isDialog()))

fun ComposeTestRule.waitForAppWindowFocus(): View = waitForWindowFocus(onRoot())

/** Compose idleness does not imply Android window focus, especially after Activity recreation. */
private fun ComposeTestRule.waitForWindowFocus(node: SemanticsNodeInteraction): View {
    val view = (node.fetchSemanticsNode().root as ViewRootForTest).view.rootView
    try {
        waitUntil(10_000) {
            runOnUiThread { view.isAttachedToWindow && view.isShown && view.hasWindowFocus() }
        }
    } catch (failure: Throwable) {
        captureWindowDiagnostics()
        throw failure
    }
    return view
}

/** Preserve the actual failing window, before test/Activity cleanup obscures the cause. */
private fun ComposeTestRule.captureWindowDiagnostics() {
    runCatching {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.getExternalFilesDir(null), "ci-diagnostics").apply { mkdirs() }
        val name = "window-focus-${SystemClock.uptimeMillis()}"
        val state = runOnUiThread {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            "interactive=${power.isInteractive}, keyguardLocked=${keyguard.isKeyguardLocked}\n" +
                WindowInspector.getGlobalWindowViews().joinToString("\n") {
                    "${it.javaClass.name}: shown=${it.isShown}, attached=${it.isAttachedToWindow}, " +
                        "focus=${it.hasWindowFocus()}, displayState=${it.display?.state}"
                }
        }
        Log.e("TestWindowDiagnostics", state)
        File(directory, "$name.txt").writeText(state)
        instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
            File(directory, "$name.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
        }
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "cp -R ${directory.absolutePath} /sdcard/Download/qt-ci-diagnostics"
        )).use { it.readBytes() }
    }.onFailure { Log.e("TestWindowDiagnostics", "Could not capture window diagnostics", it) }
}
