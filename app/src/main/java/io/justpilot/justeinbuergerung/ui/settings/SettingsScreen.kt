package io.justpilot.justeinbuergerung.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.BuildConfig
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.Settings
import io.justpilot.justeinbuergerung.domain.model.ThemeMode
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.FederalStateItem
import io.justpilot.justeinbuergerung.ui.common.LargeTitleScaffold
import io.justpilot.justeinbuergerung.ui.common.federalStatesByName
import io.justpilot.justeinbuergerung.ui.common.hint
import io.justpilot.justeinbuergerung.ui.common.title
import io.justpilot.justeinbuergerung.ui.theme.colorScheme
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
            onThemeMode = viewModel::setThemeMode,
            onPalette = viewModel::setPalette,
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
    onThemeMode: (ThemeMode) -> Unit,
    onPalette: (Palette) -> Unit,
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
            SectionHeader(stringResource(R.string.settings_section_learning))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_state)) },
                supportingContent = { Text(settings.federalState?.germanName.orEmpty()) },
                modifier = Modifier.clickable { pickState = true },
            )
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
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_haptics)) },
                supportingContent = { Text(stringResource(R.string.settings_haptics_hint)) },
                trailingContent = { Switch(checked = settings.hapticsEnabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(settings.hapticsEnabled, role = Role.Switch, onValueChange = onHaptics),
            )

            SectionHeader(stringResource(R.string.settings_section_appearance))
            ListItem(headlineContent = { Text(stringResource(R.string.settings_theme)) })
            ThemeModeChoice(settings.themeMode, onThemeMode)
            ListItem(headlineContent = { Text(stringResource(R.string.settings_palette)) })
            PaletteChoice(settings.palette, onPalette)

            SectionHeader(stringResource(R.string.settings_section_data))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.settings_reset_hint)) },
                modifier = Modifier.clickable { confirmReset = true },
            )

            SectionHeader(stringResource(R.string.settings_section_info))
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
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp).semantics { heading() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeChoice(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
            ) {
                Text(mode.title(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** One tile per palette with a round preview of its colours, as in the system's wallpaper settings. */
@Composable
private fun PaletteChoice(selected: Palette, onSelect: (Palette) -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    Row(
        Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Palette.entries.forEach { palette ->
            val isSelected = palette == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(palette) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Swatch(colorScheme(darkTheme, dynamicColor = palette == Palette.DYNAMIC), isSelected)
                Text(palette.title(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Text(
                    palette.hint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Swatch(colors: ColorScheme, selected: Boolean) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier.size(64.dp).border(if (selected) 3.dp else 1.dp, ring, CircleShape).padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Angles start at three o'clock and run clockwise: primary on top, then tertiary and secondary.
            drawArc(colors.primary, startAngle = 180f, sweepAngle = 180f, useCenter = true)
            drawArc(colors.tertiary, startAngle = 0f, sweepAngle = 90f, useCenter = true)
            drawArc(colors.secondary, startAngle = 90f, sweepAngle = 90f, useCenter = true)
        }
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check_circle_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

@Composable
private fun LinkItem(title: String, supporting: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = { Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
