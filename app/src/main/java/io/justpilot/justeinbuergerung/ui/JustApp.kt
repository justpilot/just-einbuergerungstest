package io.justpilot.justeinbuergerung.ui

import android.app.UiModeManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.model.Palette
import io.justpilot.justeinbuergerung.domain.model.ThemeMode
import io.justpilot.justeinbuergerung.ui.catalog.CatalogScreen
import io.justpilot.justeinbuergerung.ui.exam.ExamHistoryScreen
import io.justpilot.justeinbuergerung.ui.exam.ExamScreen
import io.justpilot.justeinbuergerung.ui.home.HomeScreen
import io.justpilot.justeinbuergerung.ui.onboarding.OnboardingScreen
import io.justpilot.justeinbuergerung.ui.session.SessionScreen
import io.justpilot.justeinbuergerung.ui.settings.SettingsScreen
import io.justpilot.justeinbuergerung.ui.theme.JustEinbuergerungstestTheme
import kotlin.reflect.KClass

@Composable
fun JustApp(viewModel: AppViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    appearance?.let { ApplicationNightMode(it.themeMode) }
    val darkTheme = when (appearance?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
    }
    JustEinbuergerungstestTheme(darkTheme = darkTheme, dynamicColor = appearance?.palette != Palette.BLUE) {
        when (state) {
            // Reading the settings takes a few milliseconds; show the plain background meanwhile.
            AppUiState.Loading -> Surface(Modifier.fillMaxSize()) {}
            AppUiState.Onboarding -> OnboardingScreen(onSelect = viewModel::selectFederalState)
            AppUiState.Ready -> MainNavigation()
        }
    }
}

/**
 * Hands a chosen light or dark mode to the system, which keeps it across starts and also uses it
 * for the splash screen and the system bars. Changing it recreates the activity.
 */
@Composable
private fun ApplicationNightMode(mode: ThemeMode) {
    val context = LocalContext.current
    LaunchedEffect(mode) {
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
    }
}

private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    HOME(HomeRoute, HomeRoute::class, R.string.nav_home, R.drawable.ic_home, R.drawable.ic_home_filled),
    CATALOG(CatalogRoute, CatalogRoute::class, R.string.nav_catalog, R.drawable.ic_menu_book, R.drawable.ic_menu_book_filled),
    EXAM(ExamRoute, ExamRoute::class, R.string.nav_exam, R.drawable.ic_quiz, R.drawable.ic_quiz_filled),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.string.nav_settings, R.drawable.ic_settings, R.drawable.ic_settings_filled),
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = TopLevelDestination.entries.firstOrNull { destination ->
        entry?.destination?.hasRoute(destination.routeClass) == true
    }

    // Only the bottom bar is placed here; every screen handles the remaining insets itself.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (current != null) BottomBar(current, navController) },
    ) { padding ->
        NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> {
                HomeScreen(
                    onStartSession = { navController.navigate(SessionRoute(it.key)) },
                    onStartExam = { navController.navigate(ExamSessionRoute) },
                )
            }
            composable<CatalogRoute> {
                CatalogScreen(onStartSession = { navController.navigate(SessionRoute(it.key)) })
            }
            composable<ExamRoute> {
                ExamHistoryScreen(onStartExam = { navController.navigate(ExamSessionRoute) })
            }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ExamSessionRoute> {
                ExamScreen(
                    onClose = { navController.popBackStack() },
                    onPractice = { navController.navigate(SessionRoute(it.key)) { popUpTo<ExamSessionRoute> { inclusive = true } } },
                )
            }
            composable<SessionRoute> { entry ->
                val route = entry.toRoute<SessionRoute>()
                SessionScreen(
                    onClose = { navController.popBackStack() },
                    // A fresh screen with a fresh ViewModel picks the next questions.
                    onRestart = { navController.navigate(route) { popUpTo<SessionRoute> { inclusive = true } } },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(current: TopLevelDestination, navController: NavHostController) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(if (selected) destination.selectedIcon else destination.icon), contentDescription = null) },
                // One line even with large fonts; a hyphen-less break ("Einstell-ungen") reads worse than an ellipsis.
                label = { Text(stringResource(destination.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}
