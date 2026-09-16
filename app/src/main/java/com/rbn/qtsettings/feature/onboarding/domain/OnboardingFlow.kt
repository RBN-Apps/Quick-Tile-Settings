package com.rbn.qtsettings.feature.onboarding.domain

/** Pages of the first run, in the order they are shown. */
enum class OnboardingStep { Welcome, Tiles, Permission, Grant, Done }

/**
 * Which pages the first run consists of, without Android or persistence.
 *
 * A device that already holds the permission never sees the two setup pages, so the flow is three
 * pages long instead of five. Moving between them is the pager's job, not this object's.
 */
object OnboardingFlow {
    fun pages(granted: Boolean): List<OnboardingStep> = if (granted)
        listOf(OnboardingStep.Welcome, OnboardingStep.Tiles, OnboardingStep.Done)
    else OnboardingStep.entries

    /**
     * Page index for [step]. A permission arriving mid-setup removes the page the user is standing
     * on, which resolves to the last page rather than to nothing.
     */
    fun indexOf(step: OnboardingStep, granted: Boolean): Int {
        val pages = pages(granted)
        val index = pages.indexOf(step)
        return if (index < 0) pages.lastIndex else index
    }
}
