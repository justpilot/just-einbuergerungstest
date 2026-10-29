package io.justpilot.justeinbuergerung.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.Language

@Composable
fun Category.title(state: FederalState): String = when (this) {
    Category.POLITICS -> stringResource(R.string.topic_politics)
    Category.HISTORY -> stringResource(R.string.topic_history)
    Category.SOCIETY -> stringResource(R.string.topic_society)
    Category.STATE -> stringResource(R.string.topic_state, state.germanName)
}

@Composable
fun Language.title(): String = when (this) {
    Language.GERMAN -> stringResource(R.string.language_de)
    Language.ENGLISH -> stringResource(R.string.language_en)
    Language.UKRAINIAN -> stringResource(R.string.language_uk)
}
