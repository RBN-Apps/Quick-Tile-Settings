package com.rbn.qtsettings.utils

object DnsHostnameValidator {
    private const val MAX_HOSTNAME_LENGTH = 253

    fun isValid(hostname: String?): Boolean {
        val value = hostname?.trim() ?: return false
        if (value.length !in 1..MAX_HOSTNAME_LENGTH) return false
        if (value.startsWith(".") || value.endsWith(".")) return false
        return value.split(".").all { label ->
            label.length in 1..63 &&
                    label.first().isLetterOrDigit() &&
                    label.last().isLetterOrDigit() &&
                    label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}
