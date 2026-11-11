package com.rbn.qtsettings

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Small source-boundary checks, intentionally independent of any architecture framework. */
class ArchitectureContractTest {
    private val sources = File("src/main/java/com/rbn/qtsettings").let {
        if (it.isDirectory) it else File("app/src/main/java/com/rbn/qtsettings")
    }
    private fun files() = sources.walkTopDown().filter { it.extension == "kt" }.toList()
    private fun assertNoViolations(violations: List<String>) =
        assertTrue(violations.joinToString("\n"), violations.isEmpty())

    @Test fun productionHasNoRetiredAggregateOrUiCompatibilityPackages() {
        assertTrue("Production source directory must exist", sources.isDirectory)
        val forbidden = Regex("\\b(PreferencesManager|MainViewModel|LegacyDnsAutomationRoot|CheckboxItem|DnsStateSelector)\\b")
        assertNoViolations(files().filter { forbidden.containsMatchIn(it.readText()) ||
            it.readText().contains("package com.rbn.qtsettings.ui.") }.map { it.path })
    }

    @Test fun domainPoliciesDoNotImportAndroidResourcesOrPresentation() {
        val forbidden = Regex("(?m)^import (android\\.|androidx\\.|.*\\.R(?:\\.|$)|.*\\.presentation\\.|.*\\.platform\\.)")
        assertNoViolations(files().filter { "/domain/" in it.path && forbidden.containsMatchIn(it.readText()) }.map { it.path })
    }

    @Test fun leafComposablesDoNotOwnViewModelsOrAccessPersistence() {
        val violations = files().filter { "/presentation/" in it.path && "@Composable" in it.readText() &&
            !it.name.endsWith("Root.kt") && !it.name.endsWith("Previews.kt") }.filter {
            Regex("\\b\\w*ViewModel\\b|\\bgetSharedPreferences\\b|\\bcontentResolver\\b|\\bpackageManager\\b").containsMatchIn(
                it.readText().lineSequence().filterNot { line -> line.trim().startsWith("*") || line.trim().startsWith("//") || line.trim().startsWith("/**") }.joinToString("\n"))
        }
        assertNoViolations(violations.map { it.path })
    }

    @Test fun materialDialogFramesAreCentralized() {
        val direct = Regex("(?<![A-Za-z])(AlertDialog|Dialog|ModalBottomSheet)\\(")
        assertNoViolations(files().filter { it.name != "AppDialog.kt" && direct.containsMatchIn(it.readText()) }.map { it.path })
    }
}
