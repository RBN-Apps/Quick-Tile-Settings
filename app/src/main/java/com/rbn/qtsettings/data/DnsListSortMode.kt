package com.rbn.qtsettings.data

enum class DnsListSortMode(val persistedValue: String) {
    MANUAL("manual"),
    ALPHABETICAL("alphabetical");

    companion object {
        fun fromPersistedValue(value: String?): DnsListSortMode =
            entries.firstOrNull { it.persistedValue == value } ?: ALPHABETICAL
    }
}
