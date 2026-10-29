package io.justpilot.justeinbuergerung.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.theme.AppTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ExamHistoryScreen(onStartExam: () -> Unit, viewModel: ExamHistoryViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExamHistoryContent(state, onStartExam)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamHistoryContent(state: ExamHistoryUiState, onStartExam: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_exam)) }) }) { padding ->
        when (state) {
            ExamHistoryUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ExamHistoryUiState.Ready -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                StartCard(onStartExam, Modifier.padding(16.dp))
                Text(
                    stringResource(R.string.exam_history),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                )
                if (state.results.isEmpty()) {
                    Text(
                        stringResource(R.string.exam_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.results.forEach { ResultItem(it) }
            }
        }
    }
}

@Composable
private fun StartCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.exam_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.exam_intro), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.exam_pass_marks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.exam_start))
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.GERMAN)

@Composable
private fun ResultItem(result: SessionResult) {
    val passed = result.correct >= ExamResult.CITIZENSHIP_PASS_MARK
    ListItem(
        headlineContent = { Text(stringResource(R.string.exam_score, result.correct, result.total)) },
        supportingContent = { Text(dateFormat.format(result.finishedAt.atZone(ZoneId.systemDefault()))) },
        leadingContent = {
            Icon(
                painterResource(if (passed) R.drawable.ic_check_circle_filled else R.drawable.ic_cancel_filled),
                contentDescription = null,
                tint = if (passed) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.error,
            )
        },
        trailingContent = {
            Text(
                stringResource(if (passed) R.string.exam_passed else R.string.exam_failed),
                style = MaterialTheme.typography.labelLarge,
                color = if (passed) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.error,
            )
        },
    )
}
