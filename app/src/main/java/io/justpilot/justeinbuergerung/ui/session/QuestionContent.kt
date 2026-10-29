package io.justpilot.justeinbuergerung.ui.session

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.Language
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A question with its picture and four answer cards. Once [selectedOptionId] is set the cards
 * show the solution and no longer react to taps.
 *
 * @param showTranslation whether texts in [translationLanguage] fold out under the German ones
 * @param revealSolution false for the exam: the chosen card is only highlighted and can still be changed
 */
@Composable
fun QuestionContent(
    question: Question,
    selectedOptionId: String?,
    translationLanguage: Language,
    showTranslation: Boolean,
    onAnswer: (AnswerOption) -> Unit,
    modifier: Modifier = Modifier,
    revealSolution: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BilingualText(question.text, translationLanguage, showTranslation, isTitle = true)
        question.imagePath?.let { AssetImage(it, stringResource(R.string.session_image)) }
        Spacer(Modifier.size(4.dp))
        question.options.forEach { option ->
            AnswerCard(
                option = option,
                state = when {
                    !revealSolution -> if (option.id == selectedOptionId) AnswerState.Selected else AnswerState.Open
                    selectedOptionId == null -> AnswerState.Open
                    question.isCorrect(option.id) -> AnswerState.Correct
                    option.id == selectedOptionId -> AnswerState.Wrong
                    else -> AnswerState.Other
                },
                translationLanguage = translationLanguage,
                showTranslation = showTranslation,
                onClick = { onAnswer(option) },
                enabled = !revealSolution || selectedOptionId == null,
            )
        }
    }
}

private enum class AnswerState { Open, Selected, Correct, Wrong, Other }

@Composable
private fun AnswerCard(
    option: AnswerOption,
    state: AnswerState,
    translationLanguage: Language,
    showTranslation: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val feedback = AppTheme.feedbackColors
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (state) {
            AnswerState.Correct -> feedback.correctContainer
            AnswerState.Wrong -> colors.errorContainer
            AnswerState.Selected -> colors.primaryContainer
            else -> colors.surfaceContainerLow
        },
        label = "answer container",
    )
    val content = when (state) {
        AnswerState.Correct -> feedback.onCorrectContainer
        AnswerState.Wrong -> colors.onErrorContainer
        AnswerState.Selected -> colors.onPrimaryContainer
        AnswerState.Other -> colors.onSurface.copy(alpha = 0.6f)
        AnswerState.Open -> colors.onSurface
    }
    val border = when (state) {
        AnswerState.Correct -> feedback.correct
        AnswerState.Wrong -> colors.error
        AnswerState.Selected -> colors.primary
        else -> colors.outlineVariant
    }
    val description = when (state) {
        AnswerState.Correct -> stringResource(R.string.session_answer_correct)
        AnswerState.Wrong -> stringResource(R.string.session_answer_wrong)
        AnswerState.Selected -> stringResource(R.string.exam_answer_selected)
        else -> null
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = BorderStroke(if (state == AnswerState.Open || state == AnswerState.Other) 1.dp else 2.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                description?.let { stateDescription = it }
            },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OptionBadge(option.id, state)
            Spacer(Modifier.width(16.dp))
            BilingualText(option.text, translationLanguage, showTranslation, isTitle = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OptionBadge(optionId: String, state: AnswerState) {
    val icon = when (state) {
        AnswerState.Correct -> R.drawable.ic_check_circle_filled
        AnswerState.Wrong -> R.drawable.ic_cancel_filled
        else -> null
    }
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        } else {
            val selected = state == AnswerState.Selected
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            ) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text(optionId, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** German text with the translation folding out underneath. */
@Composable
private fun BilingualText(
    text: LocalizedText,
    translationLanguage: Language,
    showTranslation: Boolean,
    isTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text.german,
            style = if (isTitle) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTitle) FontWeight.Medium else null,
        )
        val translated = text[translationLanguage]
        AnimatedVisibility(visible = showTranslation && translated != null) {
            Text(
                translated.orEmpty(),
                style = if (isTitle) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                // Follows the card's content colour, so it dims and recolours with the German text.
                color = LocalContentColor.current.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** A picture from the app's assets, decoded off the main thread. */
@Composable
private fun AssetImage(path: String, contentDescription: String) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }
    }
    bitmap?.let {
        Surface(shape = MaterialTheme.shapes.medium, color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Image(
                it,
                contentDescription = contentDescription,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}
