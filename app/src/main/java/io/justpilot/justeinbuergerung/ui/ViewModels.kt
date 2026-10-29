package io.justpilot.justeinbuergerung.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import io.justpilot.justeinbuergerung.AppContainer
import io.justpilot.justeinbuergerung.JustApplication
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.ui.catalog.CatalogViewModel
import io.justpilot.justeinbuergerung.ui.exam.ExamHistoryViewModel
import io.justpilot.justeinbuergerung.ui.exam.ExamViewModel
import io.justpilot.justeinbuergerung.ui.home.HomeViewModel
import io.justpilot.justeinbuergerung.ui.session.SessionViewModel
import io.justpilot.justeinbuergerung.ui.settings.SettingsViewModel

/** Creates every ViewModel of the app from the [AppContainer]. */
object ViewModels {
    val factory = viewModelFactory {
        initializer { AppViewModel(container.settingsRepository) }
        initializer { HomeViewModel(container.questionRepository, container.progressRepository, container.settingsRepository, container.clock) }
        initializer { CatalogViewModel(container.questionRepository, container.progressRepository, container.settingsRepository, container.clock) }
        initializer { SettingsViewModel(container.settingsRepository, container.progressRepository) }
        initializer { ExamHistoryViewModel(container.progressRepository) }
        initializer {
            ExamViewModel(container.questionRepository, container.progressRepository, container.settingsRepository, container.clock)
        }
        initializer {
            SessionViewModel(
                selection = SessionSelection.fromKey(createSavedStateHandle().toRoute<SessionRoute>().selection),
                questionRepository = container.questionRepository,
                progressRepository = container.progressRepository,
                settingsRepository = container.settingsRepository,
                clock = container.clock,
            )
        }
    }

    private val CreationExtras.container: AppContainer
        get() = (checkNotNull(this[APPLICATION_KEY]) as JustApplication).container
}
