package com.rbn.qtsettings

/**
 * Marks gallery variants whose only purpose is rendering review screenshots: without the
 * screenshot arguments they assert no more than their assertion-bearing siblings. CI excludes
 * them via the `notAnnotation` runner argument; run them together with the screenshot argument.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ScreenshotLane
