package com.rbn.qtsettings

/**
 * Marks test classes whose outcome depends on the device window: the adaptive rail, real edge
 * gestures, dialog and IME windows, rotation and recreation of the whole app shell. CI runs the
 * full suite on the phone profile and only these classes on the tablet and foldable profiles.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class AdaptiveLayout
