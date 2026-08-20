# Instrumentation dependencies shared with the target APK are omitted from the test APK.
# AndroidX TestDirCalculator runs in the target process and requires kotlin.LazyKt during
# runner initialization.
-keep class androidx.tracing.** { *; }
-keep class kotlin.LazyKt { *; }

# Entry points invoked by the minified instrumentation test.
-keep class com.rbn.qtsettings.BackupRestoreTestBridgeKt* { *; }
