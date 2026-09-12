package com.rbn.qtsettings.core.presentation

import android.content.res.Resources

/** Resolve on delivery so configuration changes do not retain a previous locale. */
data class UiMessage(val resource: Int, val argument: Any? = null) {
    fun resolve(resources: Resources): String = if (argument == null) resources.getString(resource)
        else resources.getString(resource, argument)
}
