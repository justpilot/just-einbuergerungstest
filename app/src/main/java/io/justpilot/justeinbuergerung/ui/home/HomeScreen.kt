package io.justpilot.justeinbuergerung.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.Activity
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.exam.ExamResult
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.common.StackedBoxBar
import io.justpilot.justeinbuergerung.ui.common.boxColors
import io.justpilot.justeinbuergerung.ui.theme.AppTheme
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onStartSession: (SessionSelection) -> Unit,
    onStartExam: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = ViewModels.factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onStartSession, onStartExam)
}

@Composable
fun HomeContent(state: HomeUiState, onStartSession: (SessionSelection) -> Unit, onStartExam: () -> Unit) {
    // No app bar: the brand header scrolls with the cards.
    Scaffold { padding ->
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
                Header(state)
                DailyCard(state.overview, state.activity, onStart = { onStartSession(SessionSelection.Daily) })
                ExamCard(state.lastExam, onStartExam)
                ProgressCard(state.overview, onPracticeBox = { onStartSession(SessionSelection.Box(it)) })
                ActivityCard(state.activity)
            }
        }
    }
}

/** App mark and name, a greeting and the facts that frame the learning: federal state and streak. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: HomeUiState.Ready) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppMark()
            Wordmark()
        }
        Text(
            stringResource(
                when (state.greeting) {
                    Greeting.MORNING -> R.string.home_greeting_morning
                    Greeting.DAY -> R.string.home_greeting_day
                    Greeting.EVENING -> R.string.home_greeting_evening
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoPill(R.drawable.ic_location_on_filled, state.federalState.germanName, MaterialTheme.colorScheme.primary)
            val streak = state.activity.streak
            if (streak > 0) {
                InfoPill(
                    R.drawable.ic_local_fire_department_filled,
                    pluralStringResource(R.plurals.home_streak, streak, streak),
                    MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

/** The launcher icon's passport on its cream background. */
@Composable
private fun AppMark() {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        // The adaptive-icon foreground keeps its artwork in the middle of a 108 dp canvas.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(54.dp))
    }
}

/** The app name with its first word ("Just") in the brand colour. */
@Composable
private fun Wordmark() {
    val name = stringResource(R.string.app_name)
    val first = name.substringBefore(' ')
    val primary = MaterialTheme.colorScheme.primary
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Bold)) { append(first) }
            append(name.removePrefix(first))
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun InfoPill(@DrawableRes icon: Int, text: String, tint: Color) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The main call to action, styled like the passport of the app icon: a deep primary card with a
 * gold button. A light theme's primary container is too pale for that, so it uses primary itself.
 */
@Composable
private fun DailyCard(overview: LearningOverview, activity: Activity, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val light = colors.surface.luminance() > 0.5f
    val container = if (light) colors.primary else colors.primaryContainer
    val content = if (light) colors.onPrimary else colors.onPrimaryContainer
    val gold = ButtonDefaults.buttonColors(
        containerColor = if (light) colors.tertiaryContainer else colors.tertiary,
        contentColor = if (light) colors.onTertiaryContainer else colors.onTertiary,
    )
    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activity.dailyDoneToday) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_check_circle_filled), contentDescription = null)
                    Text(stringResource(R.string.home_daily_done_title), style = MaterialTheme.typography.titleLarge)
                }
                // Wrong answers land in box 1, which is due at once, so "due" is not a to-do here.
                Text(
                    if (overview.due > 0) pluralStringResource(R.plurals.home_daily_done_due, overview.due, overview.due)
                    else stringResource(R.string.home_daily_done_text),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = onStart,
                    border = BorderStroke(1.dp, content.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.home_daily_again))
                }
            } else {
                Text(stringResource(R.string.home_daily_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_daily_text), style = MaterialTheme.typography.bodyMedium)
                Text(
                    when {
                        overview.due > 0 -> pluralStringResource(R.plurals.home_due, overview.due, overview.due)
                        overview.new == overview.total -> stringResource(R.string.home_first_session)
                        else -> stringResource(R.string.home_nothing_due)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (activity.streak > 0) {
                    Text(stringResource(R.string.home_streak_keep), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Button(onClick = onStart, colors = gold, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(painterResource(R.drawable.ic_play_arrow_filled), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(stringResource(R.string.home_daily_start), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
        }
    }
}

/** Shortcut to the exam simulation with the last result, so readiness can be checked from Home. */
@Composable
private fun ExamCard(lastExam: SessionResult?, onStart: () -> Unit) {
    ElevatedCard {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.exam_title), style = MaterialTheme.typography.titleMedium)
                if (lastExam == null) {
                    Text(
                        stringResource(R.string.home_exam_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val passed = lastExam.correct >= ExamResult.CITIZENSHIP_PASS_MARK
                    Text(
                        stringResource(
                            if (passed) R.string.home_exam_last_passed else R.string.home_exam_last_failed,
                            lastExam.correct,
                            lastExam.total,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (passed) AppTheme.feedbackColors.correct else MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedButton(onClick = onStart, modifier = Modifier.padding(start = 12.dp)) {
                Icon(painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(R.string.home_exam_start), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}

@Composable
private fun ProgressCard(overview: LearningOverview, onPracticeBox: (box: Int) -> Unit) {
    val colors = boxColors()
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_progress_title), style = MaterialTheme.typography.titleMedium)
            // Box 2 counts as progress too: the mastered count alone stays at 0 for the first days.
            Column {
                Text(stringResource(R.string.home_learned, overview.learned, overview.total), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.home_learned_mastered, overview.mastered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StackedBoxBar(overview, colors = colors)
            BoxLadder(overview, colors, onPracticeBox)
            NextGoal(overview.nextGoal)
            Text(
                stringResource(R.string.home_boxes_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityCard(activity: Activity) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val total = activity.days.sumOf { it.answers }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.home_activity_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    pluralStringResource(R.plurals.home_activity_total, total, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ActivityChart(activity.days)
        }
    }
}

private val CHART_HEIGHT = 88.dp

/**
 * Answers per day as bars on a shared baseline. Only the selected day (today by default) carries
 * its number; tapping a bar selects it.
 */
@Composable
private fun ActivityChart(days: List<Activity.Day>) {
    var selected by rememberSaveable(days.size) { mutableIntStateOf(days.lastIndex) }
    val max = days.maxOf { it.answers }.coerceAtLeast(1)
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            val dayName = if (isToday) stringResource(R.string.home_activity_today)
            else day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
            val description = pluralStringResource(R.plurals.home_activity_day, day.answers, dayName, day.answers)
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { selected = index }
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        this.selected = index == selected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (index == selected) "${day.answers}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Box(Modifier.height(CHART_HEIGHT).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (day.answers > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                // Small days stay visible as a stub.
                                .height((CHART_HEIGHT * day.answers / max).coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (index == selected) colors.primary else colors.primary.copy(alpha = 0.55f)),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).removeSuffix("."),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else null,
                    color = if (isToday) colors.onSurface else colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** New › box 1 › box 2 › mastered: the path every question takes. Boxes with questions start a session of that box. */
@Composable
private fun BoxLadder(overview: LearningOverview, colors: List<Color>, onPracticeBox: (box: Int) -> Unit) {
    // Top-aligned so the arrows sit on the line of the counts, also when labels wrap.
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        BoxCount(stringResource(R.string.home_box_new), overview.new, MaterialTheme.colorScheme.surfaceVariant)
        overview.perBox.forEachIndexed { index, count ->
            Text(
                "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp).clearAndSetSemantics {},
            )
            BoxCount(
                stringResource(R.string.home_box, index + 1),
                count,
                colors[index],
                mastered = index == overview.perBox.lastIndex,
                onClick = { onPracticeBox(index + 1) }.takeIf { count > 0 },
                clickLabel = stringResource(R.string.home_box_practice, index + 1),
            )
        }
    }
}

@Composable
private fun RowScope.BoxCount(
    label: String,
    count: Int,
    color: Color,
    mastered: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    Column(
        Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = clickLabel, onClick = onClick) else Modifier)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (mastered) {
                Icon(painterResource(R.drawable.ic_star_filled), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            } else {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            }
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                color = if (mastered) color else Color.Unspecified,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NextGoal(goal: LearningOverview.NextGoal) {
    val text = when (goal) {
        is LearningOverview.NextGoal.ReadyToMaster -> pluralStringResource(R.plurals.home_goal_ready, goal.count, goal.count)
        is LearningOverview.NextGoal.ReadySoon ->
            if (goal.inDays == 1) pluralStringResource(R.plurals.home_goal_ready_tomorrow, goal.count, goal.count)
            else pluralStringResource(R.plurals.home_goal_ready_in_days, goal.count, goal.count, goal.inDays)
        is LearningOverview.NextGoal.Milestone -> pluralStringResource(R.plurals.home_goal_milestone, goal.missing, goal.missing, goal.target)
        LearningOverview.NextGoal.AllMastered -> stringResource(R.string.home_goal_all_mastered)
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_star_filled), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
