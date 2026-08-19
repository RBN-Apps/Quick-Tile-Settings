package com.rbn.qtsettings.utils

import android.content.Context
import androidx.core.content.edit

/** Coordinates pending tile auto-reverts with direct actions performed elsewhere in the app. */
internal object AutoRevertCoordinator {
    private const val PREFERENCES_NAME = "auto_revert_coordination"
    private const val KEY_DNS_GENERATION = "dns_generation"
    private const val KEY_USB_GENERATION = "usb_generation"

    fun dnsGeneration(context: Context): Long = generation(context, KEY_DNS_GENERATION)

    fun usbGeneration(context: Context): Long = generation(context, KEY_USB_GENERATION)

    fun invalidateDnsAutoRevert(context: Context) {
        incrementGeneration(context, KEY_DNS_GENERATION)
    }

    fun invalidateUsbAutoRevert(context: Context) {
        incrementGeneration(context, KEY_USB_GENERATION)
    }

    private fun generation(context: Context, key: String): Long =
        preferences(context).getLong(key, 0L)

    private fun incrementGeneration(context: Context, key: String) {
        val preferences = preferences(context)
        preferences.edit { putLong(key, preferences.getLong(key, 0L) + 1L) }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
