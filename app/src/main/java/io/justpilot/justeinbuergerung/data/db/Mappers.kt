package io.justpilot.justeinbuergerung.data.db

import io.justpilot.justeinbuergerung.domain.model.AnswerOption
import io.justpilot.justeinbuergerung.domain.model.Category
import io.justpilot.justeinbuergerung.domain.model.FederalState
import io.justpilot.justeinbuergerung.domain.model.LocalizedText
import io.justpilot.justeinbuergerung.domain.model.Progress
import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.model.SessionKind
import io.justpilot.justeinbuergerung.domain.model.SessionResult
import java.time.Instant
import java.time.LocalDate

fun Question.toEntities() = QuestionWithOptions(
    question = QuestionEntity(
        id = id,
        number = number,
        state = state?.code,
        category = category.code,
        imagePath = imagePath,
        textDe = text.german,
        textEn = text.english,
        textUk = text.ukrainian,
        correctOption = correctOptionId,
    ),
    options = options.mapIndexed { position, option ->
        OptionEntity(
            questionId = id,
            optionId = option.id,
            position = position,
            textDe = option.text.german,
            textEn = option.text.english,
            textUk = option.text.ukrainian,
        )
    },
)

fun QuestionWithOptions.toDomain() = Question(
    id = question.id,
    number = question.number,
    state = question.state?.let(FederalState::fromCode),
    category = Category.fromCode(question.category),
    imagePath = question.imagePath,
    text = LocalizedText(question.textDe, question.textEn, question.textUk),
    options = options.sortedBy { it.position }.map {
        AnswerOption(it.optionId, LocalizedText(it.textDe, it.textEn, it.textUk))
    },
    correctOptionId = question.correctOption,
)

fun ProgressEntity.toDomain() = Progress(
    questionId = questionId,
    box = box,
    dueDate = dueEpochDay?.let(LocalDate::ofEpochDay),
    lastAnsweredAt = lastAnsweredAt?.let(Instant::ofEpochMilli),
    bookmarked = bookmarked,
)

fun SessionResultEntity.toDomain() = SessionResult(
    kind = SessionKind.valueOf(kind),
    finishedAt = Instant.ofEpochMilli(finishedAt),
    day = LocalDate.ofEpochDay(finishedEpochDay),
    correct = correct,
    total = total,
)
