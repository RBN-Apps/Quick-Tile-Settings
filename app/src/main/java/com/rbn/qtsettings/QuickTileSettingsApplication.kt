package com.rbn.qtsettings

import android.app.Application
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi

class QuickTileSettingsApplication : Application() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate() {
        super.onCreate()
        // Compose UI 1.12.0 can dispatch StopInput before the next field's StartInput,
        // briefly hiding the IME during focus transfers (notably Samsung Keyboard).
        // Use frame-batched input commands until our stable Compose version includes:
        // https://android.googlesource.com/platform/frameworks/support/+/f3d1e1632c81ceeec2a0cd5f14a3ef985d0f8f80
        // Remove this workaround after upgrading and rechecking field switches and Back.
        AndroidComposeUiFlags.isOutOfFrameSchedulerForTextInputEventsEnabled = false
    }
}
