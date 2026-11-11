package com.rbn.qtsettings.app

import android.content.ComponentName
import android.content.Intent
import com.rbn.qtsettings.app.navigation.AppArea
import com.rbn.qtsettings.app.navigation.resolveTileArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileEntryResolverTest {
    @Test fun componentAndStringFormsResolveBothAreas() {
        for ((service, area) in listOf("PrivateDnsTileService" to AppArea.Dns, "UsbDebuggingTileService" to AppArea.Usb)) {
            val component = ComponentName("com.rbn.qtsettings", "com.rbn.qtsettings.services.$service")
            assertEquals(area, Intent().putExtra(Intent.EXTRA_COMPONENT_NAME, component).resolveTileArea())
            assertEquals(area, Intent().putExtra(Intent.EXTRA_COMPONENT_NAME, component.flattenToString()).resolveTileArea())
            assertEquals(area, Intent().putExtra(Intent.EXTRA_COMPONENT_NAME, component.className).resolveTileArea())
        }
    }

    @Test fun absentUnknownAndWrongTypeExtrasDoNotRedirect() {
        assertNull(Intent().resolveTileArea())
        assertNull(Intent().putExtra(Intent.EXTRA_COMPONENT_NAME, "unrelated.Component").resolveTileArea())
        assertNull(Intent().putExtra(Intent.EXTRA_COMPONENT_NAME, 42).resolveTileArea())
    }
}
