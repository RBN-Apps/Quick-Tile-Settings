package com.rbn.qtsettings

import android.os.ParcelFileDescriptor
import android.view.Choreographer
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.rbn.qtsettings.core.designsystem.theme.QuickTileSettingsTheme
import com.rbn.qtsettings.data.DnsHostnameEntry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

@AdaptiveLayout
class DnsHostnameImeInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private data class ImeFrame(val visible: Boolean, val bottom: Int, val height: Int, val top: Int)

    // Run on an explicitly selected emulator with a software IME enabled.
    // Sample real rendered frames: a final assertIsFocused would miss a brief IME hide.
    @Test fun switchingFields_keepsImeVisibleAndDialogStable() {
        compose.setContent {
            QuickTileSettingsTheme {
                DnsHostnameEditDialog(
                    entry = DnsHostnameEntry(name = "Home DNS", hostname = "dns.example.com"),
                    onDismiss = {}, onSave = { _, _, _ -> }
                )
            }
        }
        val window = compose.waitForDialogWindowFocus()
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTouchInput { click() }
        compose.waitUntil(30_000) {
            ViewCompat.getRootWindowInsets(window)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        val frames = CopyOnWriteArrayList<ImeFrame>()
        var recording = true
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(time: Long) {
                val insets = ViewCompat.getRootWindowInsets(window)
                val position = IntArray(2)
                window.getLocationOnScreen(position)
                frames += ImeFrame(
                    visible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true,
                    bottom = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0,
                    height = window.height,
                    top = position[1]
                )
                if (recording) Choreographer.getInstance().postFrameCallback(this)
            }
        }
        compose.runOnIdle { Choreographer.getInstance().postFrameCallback(callback) }
        try {
            compose.waitUntil(10_000) { frames.size >= 45 }
            fields[0].performTextReplacement("Updated DNS")
            frames.clear()
            repeat(6) { index ->
                fields[(index + 1) % 2].performTouchInput { click() }
                fields[(index + 1) % 2].assertIsFocused()
                val start = frames.size
                compose.waitUntil(5_000) { frames.size >= start + 30 }
            }
            val hiddenFrames = frames.count { !it.visible }
            val heights = frames.map { it.height }.distinct()
            val bottoms = frames.map { it.bottom }.distinct()
            val tops = frames.map { it.top }.distinct()
            android.util.Log.i("DnsImeRegression", "frames=${frames.size}, hidden=$hiddenFrames, heights=$heights, imeBottoms=$bottoms, tops=$tops")
            assertTrue("IME disappeared in $hiddenFrames frames", hiddenFrames == 0)
            assertTrue("Dialog height changed during focus switches: $heights", heights.size == 1)
            assertTrue("Keyboard animated during focus switches: $bottoms", bottoms.size == 1)
            assertTrue("Dialog moved during focus switches: $tops", tops.size == 1)
        } finally {
            compose.runOnIdle {
                recording = false
                Choreographer.getInstance().removeFrameCallback(callback)
            }
        }
        fields[0].assertTextContains("Updated DNS")
        fields[1].assertTextContains("dns.example.com")

        // The fallback must still allow intentional keyboard dismissal and reopening.
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("input keyevent KEYCODE_BACK")
        ).use { it.readBytes() }
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(window)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        fields[0].performTouchInput { click() }
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(window)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        fields[0].assertTextContains("Updated DNS")
    }
}
