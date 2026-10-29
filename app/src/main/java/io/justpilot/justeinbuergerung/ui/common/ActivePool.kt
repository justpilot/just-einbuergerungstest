package io.justpilot.justeinbuergerung.ui.common

import io.justpilot.justeinbuergerung.domain.model.Question
import io.justpilot.justeinbuergerung.domain.repository.QuestionRepository
import io.justpilot.justeinbuergerung.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The question pool of the selected federal state; empty while none is selected or the catalog is importing. */
@OptIn(ExperimentalCoroutinesApi::class)
fun activePool(settingsRepository: SettingsRepository, questionRepository: QuestionRepository): Flow<List<Question>> =
    settingsRepository.settings
        .map { it.federalState }
        .distinctUntilChanged()
        .flatMapLatest { state -> if (state == null) flowOf(emptyList()) else questionRepository.questionPool(state) }
