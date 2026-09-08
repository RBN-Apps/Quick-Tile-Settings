package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.LegacyDestinationContent

/** App chrome only. Its slots are wired by AppRoot, never by feature screens. */
@Composable
fun AppScaffold(
    destination: AppDestination,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenDestination: (AppDestination) -> Unit,
    onShowPermissionHelp: () -> Unit,
    onShowAbout: () -> Unit,
    content: @Composable (AppDestination) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AppTopBar(destination, onBack, onOpenDestination, onShowPermissionHelp, onShowAbout) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LegacyDestinationContent(destination, Modifier.padding(padding).fillMaxSize(), content)
    }
}
