package com.rbn.qtsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rbn.qtsettings.data.DnsHostnameEntry
import com.rbn.qtsettings.feature.dns.domain.validateDnsEditor
import com.rbn.qtsettings.feature.dns.presentation.DnsEditorState

import com.rbn.qtsettings.feature.dns.presentation.DnsHostnameEditForm

/** State fixture for the IME regression test; production editing is ViewModel-owned. */
@Composable
fun DnsHostnameEditDialog(
    entry: DnsHostnameEntry?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, hostname: String) -> Unit
) {
    var name by rememberSaveable(entry?.id) { mutableStateOf(entry?.name.orEmpty()) }
    var hostname by rememberSaveable(entry?.id) { mutableStateOf(entry?.hostname.orEmpty()) }
    var errors by remember { mutableStateOf(validateDnsEditor("valid", "valid")) }
    DnsHostnameEditForm(
        DnsEditorState(true, entry?.id, name, hostname, errors.nameError, errors.hostnameError),
        { name = it; errors = errors.copy(nameError = null) },
        { hostname = it; errors = errors.copy(hostnameError = null) },
        onDismiss,
        {
            name = name.trim(); hostname = hostname.trim()
            errors = validateDnsEditor(name, hostname)
            if (errors.valid) { onSave(entry?.id, name, hostname); onDismiss() }
        }
    )
}
