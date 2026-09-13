package com.rbn.qtsettings

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import org.robolectric.RuntimeEnvironment
import com.rbn.qtsettings.platform.app.openAppProjectLink
import com.rbn.qtsettings.platform.app.readInstalledAppInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidAppInfoTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun readsInstalledVersionAndPublicDeviceMetadata() {
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        val result = readInstalledAppInfo(context)
        assertEquals(installed.versionName, result.details.versionName)
        assertEquals(installed.longVersionCode, result.details.versionCode)
        assertEquals(context.packageName, result.details.packageName)
        assertEquals(Build.VERSION.SDK_INT, result.details.apiLevel)
        assertEquals(Build.SUPPORTED_ABIS.joinToString(", "), result.details.abis)
        assertNotNull(result.icon)
    }

    @Test fun missingOrBlockedBrowserIsReportedToCaller() {
        for (exception in listOf(ActivityNotFoundException(), SecurityException())) {
            val unavailable = object : ContextWrapper(context) {
                override fun startActivity(intent: Intent) { throw exception }
            }
            assertFalse(openAppProjectLink(unavailable, "https://github.com/RBN-Apps/Quick-Tile-Settings"))
        }
    }
}
