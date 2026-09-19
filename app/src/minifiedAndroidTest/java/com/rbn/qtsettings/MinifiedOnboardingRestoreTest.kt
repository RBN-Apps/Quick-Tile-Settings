package com.rbn.qtsettings

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class MinifiedOnboardingRestoreTest {
    @Test fun pagesMethodsAndTilesRestoreFromParcelInShrunkTarget() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            instrumentation.targetContext.classLoader
                .loadClass("com.rbn.qtsettings.OnboardingRestoreTestBridgeKt")
                .getMethod("verifyMinifiedOnboardingRestore").invoke(null)
        }
    }
}
