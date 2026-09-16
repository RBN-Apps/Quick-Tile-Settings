package com.rbn.qtsettings.feature.onboarding

import com.rbn.qtsettings.feature.onboarding.domain.OnboardingFlow
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import org.junit.Assert.*
import org.junit.Test

class OnboardingFlowTest {
    @Test fun missingPermission_keepsEveryPageInItsDeclaredOrder() {
        assertEquals(OnboardingStep.entries, OnboardingFlow.pages(granted = false))
        OnboardingStep.entries.forEachIndexed { index, step ->
            assertEquals(index, OnboardingFlow.indexOf(step, granted = false))
        }
    }

    @Test fun grantedPermission_dropsBothSetupPages() {
        assertEquals(listOf(OnboardingStep.Welcome, OnboardingStep.Tiles, OnboardingStep.Done),
            OnboardingFlow.pages(granted = true))
        assertEquals(0, OnboardingFlow.indexOf(OnboardingStep.Welcome, granted = true))
        assertEquals(1, OnboardingFlow.indexOf(OnboardingStep.Tiles, granted = true))
        assertEquals(2, OnboardingFlow.indexOf(OnboardingStep.Done, granted = true))
    }

    /** A permission arriving on the setup page removes the page the user is standing on. */
    @Test fun pageDroppedUnderTheUser_resolvesToTheLastPage() {
        assertEquals(2, OnboardingFlow.indexOf(OnboardingStep.Permission, granted = true))
        assertEquals(2, OnboardingFlow.indexOf(OnboardingStep.Grant, granted = true))
    }

    @Test fun everyIndexAddressesAPage() {
        listOf(true, false).forEach { granted ->
            val pages = OnboardingFlow.pages(granted)
            pages.forEach { step -> assertEquals(step, pages[OnboardingFlow.indexOf(step, granted)]) }
        }
    }
}
