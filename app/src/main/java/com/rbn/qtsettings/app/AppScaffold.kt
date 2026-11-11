package com.rbn.qtsettings.app

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.rbn.qtsettings.app.navigation.AppRailTarget
import com.rbn.qtsettings.app.navigation.AppDestination
import com.rbn.qtsettings.app.navigation.AppNavigationScaffold

/** App chrome only. Its slots are wired by AppRoot, never by feature screens. */
@Composable
fun AppScaffold(
    destination: AppDestination,
    selectedTarget: AppRailTarget,
    onSelectTarget: (AppRailTarget) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val density = LocalDensity.current
    AppNavigationScaffold(selectedTarget, onSelectTarget) { compact, openNavigation, onHeaderHeight ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                AppTopBar(
                    modifier = Modifier.onSizeChanged { onHeaderHeight(with(density) { it.height.toDp() }) },
                    destination = destination,
                    selectedTarget = selectedTarget,
                    onBack = onBack,
                    onOpenNavigation = if (compact) openNavigation else null
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            content(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize())
        }
    }
}
