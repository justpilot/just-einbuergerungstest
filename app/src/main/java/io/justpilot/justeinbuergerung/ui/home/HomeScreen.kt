package io.justpilot.justeinbuergerung.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.ui.ViewModels

@Composable
fun HomeScreen(onStartDaily: () -> Unit, viewModel: HomeViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onStartDaily)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(state: HomeUiState, onStartDaily: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        when (state) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading_catalog))
                }
            }
            is HomeUiState.Ready -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DailyCard(state.overview, onStartDaily)
                ProgressCard(state.overview)
            }
        }
    }
}

@Composable
private fun DailyCard(overview: LearningOverview, onStart: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_daily_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.home_daily_text), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (overview.due > 0) pluralStringResource(R.plurals.home_due, overview.due, overview.due)
                else stringResource(R.string.home_nothing_due),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_play_arrow_filled), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.home_daily_start), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}

@Composable
private fun ProgressCard(overview: LearningOverview) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_progress_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.home_mastered, overview.mastered, overview.total), style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(
                progress = { overview.mastered / overview.total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BoxCount(stringResource(R.string.home_box_new), overview.new)
                overview.perBox.forEachIndexed { index, count -> BoxCount(stringResource(R.string.home_box, index + 1), count) }
            }
            Text(
                stringResource(R.string.home_boxes_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.BoxCount(label: String, count: Int) {
    Column(
        Modifier.weight(1f).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}
