# Instrumentation dependencies shared with the target APK are omitted from the test APK.
# AndroidX TestDirCalculator runs in the target process and requires kotlin.LazyKt during
# runner initialization.
-keep class androidx.tracing.** { *; }
-keep class kotlin.LazyKt { *; }

# Entry points invoked by the minified instrumentation test.
-keep class com.rbn.qtsettings.BackupRestoreTestBridgeKt* { *; }

# Keep only the reflective test entry point; navigation route serializers remain shrinkable.
-keep class com.rbn.qtsettings.NavigationRestoreTestBridgeKt { public static void verifyMinifiedNavigationRestore(); }

# The onboarding model and its enums remain shrinkable; only the test entry point is retained.
-keep class com.rbn.qtsettings.OnboardingRestoreTestBridgeKt { public static void verifyMinifiedOnboardingRestore(); }
