package com.rbn.qtsettings.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.robolectric.shadows.ShadowContentResolver

/** Supplies real values through call() while Robolectric's Settings.Global stays at 0. */
class DebuggingSettingsProvider : ContentProvider() {
    val values = mutableMapOf<String, String>()
    val requestedKeys = mutableListOf<String?>()
    var failure: RuntimeException? = null
    var returnNull = false

    fun register(context: Context) {
        attachInfo(context, ProviderInfo().apply { authority = "settings" })
        ShadowContentResolver.registerProviderInternal("settings", this)
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        assertEquals("GET_global", method)
        assertNull("Generation tracking would redact the fallback too", extras)
        requestedKeys += arg
        failure?.let { throw it }
        if (returnNull) return null
        return Bundle().apply { values[arg]?.let { putString("value", it) } }
    }

    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = error("Unexpected query")
    override fun getType(uri: Uri): String? = error("Unexpected getType")
    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Unexpected insert")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("Unexpected delete")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("Unexpected update")
}
