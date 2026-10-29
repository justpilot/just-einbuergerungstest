package io.justpilot.justeinbuergerung.ui.session

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.SessionBalance
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.label
import io.justpilot.justeinbuergerung.ui.theme.AppTheme

@Composable
fun SessionScreen(onClose: () -> Unit, onRestart: () -> Unit, viewModel: SessionViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionContent(
        state = state,
        onAnswer = viewModel::answer,
        onNext = viewModel::next,
        onToggleTranslation = viewModel::toggleTranslation,
        onToggleBookmark = viewModel::toggleBookmark,
        onClose = onClose,
        onRestart = onRestart,
    )
}

/** Share of first-try answers from which a session counts as done well rather than as practice. */
private const val GOOD_SHARE = 0.7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionContent(
    state: SessionUiState,
    onAnswer: (optionId: String) -> Unit,
    onNext: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleBookmark: () -> Unit,
    onClose: () -> Unit,
    onRestart: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var confirmClose by rememberSaveable { mutableStateOf(false) }
    val asking = state as? SessionUiState.Asking
    val askFirst = asking != null && asking.isDaily && asking.isStarted
    val close = { if (askFirst) confirmClose = true else onClose() }
    BackHandler(enabled = askFirst) { confirmClose = true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (state is SessionUiState.Asking) {
                            Text(
                                stringResource(
                                    if (state.isRetryRound) R.string.session_retry_progress else R.string.session_progress,
                                    state.position,
                                    state.roundSize,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = close) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.session_close))
                        }
                    },
                    actions = {
                        if (state is SessionUiState.Asking) {
                            IconToggleButton(checked = state.showTranslation, onCheckedChange = { onToggleTranslation() }) {
                                Icon(
                                    painterResource(R.drawable.ic_translate),
                                    contentDescription = stringResource(
                                        if (state.showTranslation) R.string.session_hide_translation else R.string.session_show_translation,
                                    ),
                                )
                            }
                            IconToggleButton(checked = state.bookmarked, onCheckedChange = { onToggleBookmark() }) {
                                Icon(
                                    painterResource(if (state.bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                                    contentDescription = stringResource(
                                        if (state.bookmarked) R.string.session_remove_bookmark else R.string.session_bookmark,
                                    ),
                                )
                            }
                        }
                    },
                )
                // In the bar rather than the content, so it stays visible while a long question scrolls.
                if (state is SessionUiState.Asking) {
                    LinearProgressIndicator(
                        progress = { (state.position - 1 + if (state.selectedOptionId != null) 1 else 0) / state.roundSize.toFloat() },
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        bottomBar = {
            if (state is SessionUiState.Asking && state.selectedOptionId != null) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        Feedback(state)
                        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text(stringResource(if (state.isLastQuestion) R.string.session_finish else R.string.session_next))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                SessionUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is SessionUiState.Finished -> Summary(state, onClose, onRestart)
                is SessionUiState.Asking -> {
                    val scroll = rememberScrollState()
                    // Every question starts at the top.
                    LaunchedEffect(state.question.id, state.isRetryRound) { scroll.scrollTo(0) }
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
                        // The catalog number helps to look a question up or to report a mistake in it.
                        Text(
                            state.question.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        QuestionContent(
                            question = state.question,
                            selectedOptionId = state.selectedOptionId,
                            translationLanguage = state.translationLanguage,
                            showTranslation = state.showTranslation,
                            onAnswer = { option ->
                                if (state.hapticsEnabled) {
                                    haptics.performHapticFeedback(
                                        if (state.question.isCorrect(option.id)) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                                    )
                                }
                                onAnswer(option.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text(stringResource(R.string.session_abort_title)) },
            text = { Text(stringResource(R.string.session_abort_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    onClose()
                }) { Text(stringResource(R.string.session_abort_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.exam_continue)) } },
        )
    }
}

@Composable
private fun Feedback(state: SessionUiState.Asking) {
    val correct = state.question.isCorrect(state.selectedOptionId!!)
    Text(
        stringResource(
            when {
                correct -> R.string.session_feedback_correct
                state.isRetryRound -> R.string.session_feedback_retry_wrong
                else -> R.string.session_feedback_wrong
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        color = if (correct) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun Summary(state: SessionUiState.Finished, onClose: () -> Unit, onRestart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.total == 0) {
            Text(stringResource(R.string.session_empty), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        } else {
            // A cheer for 3 of 10 would ring hollow; a weaker round is simply finished.
            val good = state.correctFirstTime >= state.total * GOOD_SHARE
            Icon(
                painterResource(if (good) R.drawable.ic_check_circle_filled else R.drawable.ic_flag_filled),
                contentDescription = null,
                tint = if (good) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                stringResource(if (good) R.string.session_done_title else R.string.session_done_title_practice),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                stringResource(R.string.session_done_score, state.correctFirstTime, state.total),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Balance(state.balance)
            if (state.correctFirstTime < state.total) {
                Text(
                    stringResource(R.string.session_done_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (state.total > 0) {
            Button(onClick = onRestart, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.session_again)) }
            TextButton(onClick = onClose) { Text(stringResource(R.string.session_back)) }
        } else {
            Button(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.session_back)) }
        }
    }
}

/** Box moves of the session; lines without questions are left out. */
@Composable
private fun Balance(balance: SessionBalance) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (balance.mastered > 0) {
            BalanceLine(R.drawable.ic_star_filled, pluralStringResource(R.plurals.session_done_mastered, balance.mastered, balance.mastered), MaterialTheme.colorScheme.primary)
        }
        if (balance.advanced > 0) {
            BalanceLine(R.drawable.ic_arrow_upward, pluralStringResource(R.plurals.session_done_advanced, balance.advanced, balance.advanced), AppTheme.feedbackColors.correct)
        }
        if (balance.backToFirstBox > 0) {
            BalanceLine(
                R.drawable.ic_arrow_downward,
                pluralStringResource(R.plurals.session_done_back, balance.backToFirstBox, balance.backToFirstBox),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalanceLine(@DrawableRes icon: Int, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
