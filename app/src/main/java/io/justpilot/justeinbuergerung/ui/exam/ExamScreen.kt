package io.justpilot.justeinbuergerung.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.label
import io.justpilot.justeinbuergerung.ui.session.QuestionContent
import io.justpilot.justeinbuergerung.ui.theme.AppTheme
import java.time.Duration

@Composable
fun ExamScreen(onClose: () -> Unit, viewModel: ExamViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExamContent(
        state = state,
        onAnswer = viewModel::answer,
        onToggleFlag = viewModel::toggleFlag,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onGoTo = viewModel::goTo,
        onSubmit = viewModel::submit,
        onClose = onClose,
    )
}

/** Time left after which the timer turns red. */
private val TIME_WARNING: Duration = Duration.ofMinutes(5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamContent(
    state: ExamUiState,
    onAnswer: (optionId: String) -> Unit,
    onToggleFlag: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoTo: (index: Int) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmAbort by rememberSaveable { mutableStateOf(false) }
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }
    var showOverview by rememberSaveable { mutableStateOf(false) }
    val running = state as? ExamUiState.Running

    // Leaving a running exam discards it, so ask first.
    BackHandler(enabled = running != null) { confirmAbort = true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (running != null) {
                            Text(
                                stringResource(R.string.session_progress, running.position, running.total),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        } else if (state is ExamUiState.Finished) {
                            Text(stringResource(R.string.exam_result_title))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (running != null) confirmAbort = true else onClose() }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.session_close))
                        }
                    },
                    actions = {
                        if (running != null) {
                            Timer(running.remaining)
                            IconToggleButton(checked = running.flagged, onCheckedChange = { onToggleFlag() }) {
                                Icon(
                                    painterResource(if (running.flagged) R.drawable.ic_flag_filled else R.drawable.ic_flag),
                                    contentDescription = stringResource(if (running.flagged) R.string.exam_unflag else R.string.exam_flag),
                                )
                            }
                            IconButton(onClick = { showOverview = true }) {
                                Icon(painterResource(R.drawable.ic_grid_view), contentDescription = stringResource(R.string.exam_overview))
                            }
                        }
                    },
                )
                // Answered share; it stays in the bar while a long question scrolls.
                if (running != null) {
                    LinearProgressIndicator(
                        progress = { (running.total - running.unanswered) / running.total.toFloat() },
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        bottomBar = {
            if (running != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = onPrevious, enabled = !running.isFirst, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = null)
                            Text(stringResource(R.string.exam_previous))
                        }
                        if (running.isLast) {
                            Button(onClick = { confirmSubmit = true }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.exam_submit))
                            }
                        } else {
                            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.exam_next))
                                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ExamUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ExamUiState.Finished -> Result(state.result, onClose)
                is ExamUiState.Running -> {
                    val scroll = rememberScrollState()
                    // Every question starts at the top.
                    LaunchedEffect(state.position) { scroll.scrollTo(0) }
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        // The exam is in German only, like the real test.
                        QuestionContent(
                            question = state.question,
                            selectedOptionId = state.selectedOptionId,
                            translationLanguage = Language.GERMAN,
                            showTranslation = false,
                            onAnswer = { onAnswer(it.id) },
                            revealSolution = false,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    if (running != null && showOverview) {
        ModalBottomSheet(onDismissRequest = { showOverview = false }) {
            Overview(
                state = running,
                onGoTo = {
                    onGoTo(it)
                    showOverview = false
                },
                onSubmit = {
                    showOverview = false
                    confirmSubmit = true
                },
            )
        }
    }
    if (running != null && confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text(stringResource(R.string.exam_submit_title)) },
            text = {
                Text(
                    if (running.unanswered > 0) {
                        pluralStringResource(R.plurals.exam_submit_unanswered, running.unanswered, running.unanswered)
                    } else {
                        stringResource(R.string.exam_submit_complete)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSubmit = false
                    onSubmit()
                }) { Text(stringResource(R.string.exam_submit)) }
            },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text(stringResource(R.string.exam_continue)) } },
        )
    }
    if (running != null && confirmAbort) {
        AlertDialog(
            onDismissRequest = { confirmAbort = false },
            title = { Text(stringResource(R.string.exam_abort_title)) },
            text = { Text(stringResource(R.string.exam_abort_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAbort = false
                    onClose()
                }) { Text(stringResource(R.string.exam_abort_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmAbort = false }) { Text(stringResource(R.string.exam_continue)) } },
        )
    }
}

@Composable
private fun Timer(remaining: Duration) {
    val text = "%d:%02d".format(remaining.toMinutes(), remaining.toSecondsPart())
    val warning = remaining < TIME_WARNING
    val description = stringResource(R.string.exam_time_left, text)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Icon(
            painterResource(R.drawable.ic_timer),
            contentDescription = null,
            tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (warning) MaterialTheme.colorScheme.error else Color.Unspecified,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Grid of all exam questions: answered ones filled, flagged ones marked, the current one outlined. */
@Composable
private fun Overview(state: ExamUiState.Running, onGoTo: (Int) -> Unit, onSubmit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.exam_overview), style = MaterialTheme.typography.titleLarge)
        Text(
            pluralStringResource(R.plurals.exam_unanswered, state.unanswered, state.unanswered),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.overview) { index, status ->
                val current = index + 1 == state.position
                val description = buildList {
                    add(stringResource(R.string.exam_overview_question, index + 1))
                    if (status.answered) add(stringResource(R.string.exam_overview_answered))
                    if (status.flagged) add(stringResource(R.string.exam_overview_flagged))
                }.joinToString(", ")
                Surface(
                    onClick = { onGoTo(index) },
                    shape = MaterialTheme.shapes.small,
                    color = if (status.answered) colors.primaryContainer else colors.surfaceContainerHigh,
                    contentColor = if (status.answered) colors.onPrimaryContainer else colors.onSurface,
                    border = if (current) BorderStroke(2.dp, colors.primary) else null,
                    modifier = Modifier.aspectRatio(1f).semantics { contentDescription = description },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // The description already names the number.
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics {})
                        if (status.flagged) {
                            Icon(
                                painterResource(R.drawable.ic_flag_filled),
                                contentDescription = null,
                                tint = colors.tertiary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(14.dp),
                            )
                        }
                    }
                }
            }
        }
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.exam_submit)) }
    }
}

@Composable
private fun Result(result: ExamResult, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.exam_score, result.correct, result.total),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                PassLine(stringResource(R.string.exam_result_citizenship, ExamResult.CITIZENSHIP_PASS_MARK), result.passedCitizenship)
                PassLine(stringResource(R.string.exam_result_life_in_germany, ExamResult.LIFE_IN_GERMANY_PASS_MARK), result.passedLifeInGermany)
            }
        }
        if (result.wrong.isEmpty()) {
            Text(stringResource(R.string.exam_result_all_correct), style = MaterialTheme.typography.titleMedium)
        } else {
            Text(stringResource(R.string.exam_result_wrong), style = MaterialTheme.typography.titleMedium)
            result.wrong.forEach { WrongAnswer(it) }
            Text(
                stringResource(R.string.exam_result_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.exam_done)) }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PassLine(label: String, passed: Boolean) {
    val color = if (passed) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.error
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(if (passed) R.drawable.ic_check_circle_filled else R.drawable.ic_cancel_filled),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(if (passed) R.string.exam_passed else R.string.exam_failed),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun WrongAnswer(answer: ExamResult.Answer) {
    val question = answer.question
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                question.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(question.text.german, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Text(
                answer.chosenOptionId?.let { stringResource(R.string.exam_result_your_answer, question.optionText(it)) }
                    ?: stringResource(R.string.exam_result_no_answer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(R.string.exam_result_correct_answer, question.optionText(question.correctOptionId)),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.feedbackColors.correct,
            )
        }
    }
}

private fun Question.optionText(optionId: String): String =
    "$optionId) " + options.first { it.id == optionId }.text.german
