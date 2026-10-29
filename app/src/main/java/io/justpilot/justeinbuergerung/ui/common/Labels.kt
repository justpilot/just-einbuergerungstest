package io.justpilot.justeinbuergerung.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.ThemeMode

@Composable
fun Category.title(state: FederalState): String = when (this) {
    Category.POLITICS -> stringResource(R.string.topic_politics)
    Category.HISTORY -> stringResource(R.string.topic_history)
    Category.SOCIETY -> stringResource(R.string.topic_society)
    Category.STATE -> stringResource(R.string.topic_state, state.germanName)
}

/** Catalog number of a question, e.g. "Frage 123" or "Bayern, Frage 4". */
@Composable
fun Question.label(): String =
    state?.let { stringResource(R.string.question_label_state, it.germanName, number) }
        ?: stringResource(R.string.question_label_general, number)

@Composable
fun Language.title(): String = when (this) {
    Language.GERMAN -> stringResource(R.string.language_de)
    Language.ENGLISH -> stringResource(R.string.language_en)
    Language.UKRAINIAN -> stringResource(R.string.language_uk)
    Language.TURKISH -> stringResource(R.string.language_tr)
}

@Composable
fun ThemeMode.title(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
fun Palette.title(): String = when (this) {
    Palette.DYNAMIC -> stringResource(R.string.palette_dynamic)
    Palette.BLUE -> stringResource(R.string.palette_blue)
}

@Composable
fun Palette.hint(): String = when (this) {
    Palette.DYNAMIC -> stringResource(R.string.palette_dynamic_hint)
    Palette.BLUE -> stringResource(R.string.palette_blue_hint)
}
