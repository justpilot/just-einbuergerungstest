package io.justpilot.justeinbuergerung.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.BuildConfig
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.FederalStateItem
import io.justpilot.justeinbuergerung.ui.common.LargeTitleScaffold
import io.justpilot.justeinbuergerung.ui.common.federalStatesByName
import io.justpilot.justeinbuergerung.ui.common.title
import kotlinx.coroutines.launch

private const val SOURCE_URL = "github.com/justpilot/just-einbuergerungstest"

// The app itself has no network access; these links open in the browser.
private const val SOURCE_LINK = "https://$SOURCE_URL"
private const val PRIVACY_LINK = "$SOURCE_LINK/blob/main/PRIVACY.md"

/** Libraries shipped in the APK; every one is under the Apache License 2.0. */
private val LIBRARIES = listOf(
    "AndroidX Activity, Core, Lifecycle, Navigation",
    "AndroidX Room, DataStore",
    "Jetpack Compose, Material 3",
    "Kotlin Standard Library, Coroutines, Serialization",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = ViewModels.factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetDone = stringResource(R.string.settings_reset_done)
    settings?.let {
        SettingsContent(
            settings = it,
            snackbar = snackbar,
            onFederalState = viewModel::setFederalState,
            onTranslationLanguage = viewModel::setTranslationLanguage,
            onHaptics = viewModel::setHapticsEnabled,
            onDynamicColor = viewModel::setDynamicColor,
            onReset = { viewModel.resetProgress { scope.launch { snackbar.showSnackbar(resetDone) } } },
        )
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    snackbar: SnackbarHostState,
    onFederalState: (FederalState) -> Unit,
    onTranslationLanguage: (Language) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var pickState by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    // Without a browser there is nothing to open; the address stays readable in the item.
    val open = { url: String -> runCatching { uriHandler.openUri(url) } }

    LargeTitleScaffold(stringResource(R.string.nav_settings), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_state)) },
                supportingContent = { Text(settings.federalState?.germanName.orEmpty()) },
                modifier = Modifier.clickable { pickState = true },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_translation)) },
                supportingContent = { Text(stringResource(R.string.settings_translation_hint)) },
            )
            Column(Modifier.selectableGroup()) {
                Language.translations.forEach { language ->
                    val selected = settings.translationLanguage == language
                    ListItem(
                        headlineContent = { Text(language.title()) },
                        leadingContent = { RadioButton(selected = selected, onClick = null) },
                        modifier = Modifier.selectable(selected, role = Role.RadioButton) { onTranslationLanguage(language) },
                    )
                }
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_haptics)) },
                supportingContent = { Text(stringResource(R.string.settings_haptics_hint)) },
                trailingContent = { Switch(checked = settings.hapticsEnabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(settings.hapticsEnabled, role = Role.Switch, onValueChange = onHaptics),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_hint)) },
                trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = null) },
                modifier = Modifier.toggleable(settings.dynamicColor, role = Role.Switch, onValueChange = onDynamicColor),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.settings_reset_hint)) },
                modifier = Modifier.clickable { confirmReset = true },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text(stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME)) },
            )
            LinkItem(stringResource(R.string.settings_source), SOURCE_URL) { open(SOURCE_LINK) }
            LinkItem(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_text)) { open(PRIVACY_LINK) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                modifier = Modifier.clickable { showLicenses = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_sources)) },
                supportingContent = { Text(stringResource(R.string.settings_sources_text)) },
            )
        }
    }

    if (pickState) {
        AlertDialog(
            onDismissRequest = { pickState = false },
            title = { Text(stringResource(R.string.settings_state)) },
            text = {
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    federalStatesByName.forEach { state ->
                        FederalStateItem(state, selected = state == settings.federalState) {
                            onFederalState(state)
                            pickState = false
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickState = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Text(stringResource(R.string.settings_licenses_text) + "\n\n" + LIBRARIES.joinToString("\n") { "• $it" })
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.close)) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; onReset() }) {
                    Text(stringResource(R.string.settings_reset_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** An entry that opens a web page; the trailing icon says it leaves the app. */
@Composable
private fun LinkItem(title: String, supporting: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = { Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
