package io.justpilot.justeinbuergerung.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.justpilot.justeinbuergerung.domain.learning.LearningOverview
import io.justpilot.justeinbuergerung.ui.theme.AppTheme

/** Colours of box 1 to the mastered box: muted mistakes, then the primary colour, then the success green. */
@Composable
fun boxColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.outline,
    MaterialTheme.colorScheme.primary,
    AppTheme.feedbackColors.correct,
)

/** One bar for a set of questions: mastered on the left, then the lower boxes, new questions as the empty track. */
@Composable
fun StackedBoxBar(overview: LearningOverview, modifier: Modifier = Modifier, colors: List<Color> = boxColors()) {
    Row(
        modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        overview.perBox.indices.reversed().forEach { index ->
            val count = overview.perBox[index]
            if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(colors[index]))
        }
        if (overview.new > 0) Spacer(Modifier.weight(overview.new.toFloat()))
    }
}
