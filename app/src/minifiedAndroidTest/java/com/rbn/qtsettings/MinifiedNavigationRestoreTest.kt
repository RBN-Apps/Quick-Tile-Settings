package com.rbn.qtsettings

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class MinifiedNavigationRestoreTest {
    @Test fun allRouteKeysRestoreFromParcelInShrunkTarget() {
        val loader = InstrumentationRegistry.getInstrumentation().targetContext.classLoader
        loader.loadClass("com.rbn.qtsettings.NavigationRestoreTestBridgeKt")
            .getMethod("verifyMinifiedNavigationRestore").invoke(null)
    }
}
