package com.rbn.qtsettings.feature.dns.domain

import com.rbn.qtsettings.utils.DnsHostnameValidator

enum class DnsEditorError { NameEmpty, HostnameEmpty, HostnameInvalid }
data class DnsEditorValidation(val nameError: DnsEditorError?, val hostnameError: DnsEditorError?) {
    val valid get() = nameError == null && hostnameError == null
}
fun validateDnsEditor(name: String, hostname: String) = DnsEditorValidation(
    nameError = if (name.isBlank()) DnsEditorError.NameEmpty else null,
    hostnameError = when {
        hostname.isBlank() -> DnsEditorError.HostnameEmpty
        !DnsHostnameValidator.isValid(hostname.trim()) -> DnsEditorError.HostnameInvalid
        else -> null
    }
)
