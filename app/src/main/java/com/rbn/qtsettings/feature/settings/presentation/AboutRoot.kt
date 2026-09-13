package com.rbn.qtsettings.feature.settings.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.rbn.qtsettings.R
import com.rbn.qtsettings.platform.app.InstalledAppInfo
import com.rbn.qtsettings.platform.app.openAppProjectLink
import com.rbn.qtsettings.platform.app.readInstalledAppInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbn.qtsettings.platform.app.bugReportUrl
import com.rbn.qtsettings.platform.app.issueTechnicalDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads immutable installation metadata off the UI thread; never reads application settings. */
@Composable
fun AboutRoot(onPermissionHelp: () -> Unit, onShortcuts: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val report: AboutReportViewModel = viewModel()
    val reportDetails by report.details.collectAsStateWithLifecycle()
    val includeDetails by report.includeDetails.collectAsStateWithLifecycle()
    val projectUrl = stringResource(R.string.github_repo_url)
    val openLink: (String) -> Boolean = { url ->
        openAppProjectLink(context, url).also { opened ->
            if (!opened) Toast.makeText(context, R.string.about_no_link_handler, Toast.LENGTH_LONG).show()
        }
    }
    val technicalTitle = stringResource(R.string.about_technical_title)
    val installation by produceState<InstalledAppInfo?>(null, context) {
        value = withContext(Dispatchers.IO) { readInstalledAppInfo(context) }
    }
    val loaded = installation
    if (loaded == null) {
        // Do not lay out placeholder rows: their different heights would clamp restored scroll.
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.about_loading))
        }
        return
    }
    AboutScreen(
        info = loaded.details,
        icon = loaded.icon?.asImageBitmap(),
        onPermissionHelp = onPermissionHelp,
        onShortcuts = onShortcuts,
        onSettings = onSettings,
        onLink = { openLink(it) },
        onReport = { report.open(issueTechnicalDetails(context, loaded.details)) },
        onCopy = { text ->
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                ClipData.newPlainText(technicalTitle, text))
            Toast.makeText(context, R.string.about_technical_copied, Toast.LENGTH_SHORT).show()
        }
    )
    reportDetails?.let { details ->
        AboutReportDialog(details, includeDetails, report::include,
            onOpen = {
                if (openLink(bugReportUrl(projectUrl, loaded.details, details.takeIf { includeDetails })))
                    report.dismiss()
            }, onDismiss = report::dismiss)
    }
}
