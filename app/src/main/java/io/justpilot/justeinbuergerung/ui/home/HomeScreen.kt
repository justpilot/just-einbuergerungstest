package io.justpilot.justeinbuergerung.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.Activity
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.ui.ViewModels
import io.justpilot.justeinbuergerung.ui.theme.AppTheme
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(onStartSession: (SessionSelection) -> Unit, viewModel: HomeViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, onStartSession)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(state: HomeUiState, onStartSession: (SessionSelection) -> Unit) {
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
                DailyCard(state.overview, onStart = { onStartSession(SessionSelection.Daily) })
                ActivityCard(state.activity)
                ProgressCard(state.overview, onPracticeBox = { onStartSession(SessionSelection.Box(it)) })
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
            StackedBoxBar(overview, colors)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painterResource(R.drawable.ic_local_fire_department_filled),
                    contentDescription = null,
                    tint = if (activity.streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
                Text(
                    pluralStringResource(R.plurals.home_streak, activity.streak, activity.streak),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                stringResource(
                    when {
                        activity.dailyDoneToday -> R.string.home_streak_done_today
                        activity.streak > 0 -> R.string.home_streak_keep
                        else -> R.string.home_streak_start
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/** Colours of box 1 to the mastered box: muted mistakes, then the primary colour, then the success green. */
@Composable
private fun boxColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.outline,
    MaterialTheme.colorScheme.primary,
    AppTheme.feedbackColors.correct,
)

/** One bar for the whole pool: mastered on the left, then the lower boxes, new questions as the empty track. */
@Composable
private fun StackedBoxBar(overview: LearningOverview, colors: List<Color>) {
    Row(
        Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        overview.perBox.indices.reversed().forEach { index ->
            val count = overview.perBox[index]
            if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(colors[index]))
        }
        if (overview.new > 0) Spacer(Modifier.weight(overview.new.toFloat()))
    }
}

/** New › box 1 › box 2 › mastered: the path every question takes. Boxes with questions start a session of that box. */
@Composable
private fun BoxLadder(overview: LearningOverview, colors: List<Color>, onPracticeBox: (box: Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        BoxCount(stringResource(R.string.home_box_new), overview.new, MaterialTheme.colorScheme.surfaceVariant)
        overview.perBox.forEachIndexed { index, count ->
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
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
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
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
