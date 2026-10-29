package io.justpilot.justeinbuergerung.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.LargeTitleScaffold
import io.justpilot.justeinbuergerung.ui.common.StackedBoxBar
import io.justpilot.justeinbuergerung.ui.common.title

@Composable
fun CatalogScreen(onStartSession: (SessionSelection) -> Unit, viewModel: CatalogViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogContent(state, onStartSession)
}

@Composable
fun CatalogContent(state: CatalogUiState, onStartSession: (SessionSelection) -> Unit) {
    LargeTitleScaffold(stringResource(R.string.nav_catalog)) { padding ->
        when (state) {
            CatalogUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogUiState.Ready -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                SectionHeader(stringResource(R.string.catalog_topics))
                state.topics.forEach { (category, overview) ->
                    ListItem(
                        headlineContent = { Text(category.title(state.state)) },
                        // Same segments as on Home, so box 2 shows as progress before anything is mastered.
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.catalog_topic_progress, overview.learned, overview.total, overview.mastered))
                                StackedBoxBar(overview, Modifier.padding(top = 8.dp))
                            }
                        },
                        modifier = Modifier.clickable { onStartSession(SessionSelection.Topic(category)) },
                    )
                }
                SectionHeader(stringResource(R.string.catalog_review))
                ReviewItem(
                    title = stringResource(R.string.catalog_mistakes),
                    count = state.mistakes,
                    countText = pluralStringResource(R.plurals.catalog_mistakes_count, state.mistakes, state.mistakes),
                    icon = R.drawable.ic_cancel_filled,
                    onClick = { onStartSession(SessionSelection.Mistakes) },
                )
                ReviewItem(
                    title = stringResource(R.string.catalog_bookmarks),
                    count = state.bookmarks,
                    countText = pluralStringResource(R.plurals.catalog_bookmarks_count, state.bookmarks, state.bookmarks),
                    icon = R.drawable.ic_bookmark_filled,
                    onClick = { onStartSession(SessionSelection.Bookmarks) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

/** An entry that is disabled while it holds no questions. */
@Composable
private fun ReviewItem(title: String, count: Int, countText: String, icon: Int, onClick: () -> Unit) {
    val enabled = count > 0
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(if (enabled) countText else stringResource(R.string.catalog_empty)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
    )
}
