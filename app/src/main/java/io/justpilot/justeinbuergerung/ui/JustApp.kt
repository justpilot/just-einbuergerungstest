package io.justpilot.justeinbuergerung.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.justpilot.justeinbuergerung.R
import io.justpilot.justeinbuergerung.domain.learning.SessionSelection
import io.justpilot.justeinbuergerung.ui.catalog.CatalogScreen
import io.justpilot.justeinbuergerung.ui.home.HomeScreen
import io.justpilot.justeinbuergerung.ui.onboarding.OnboardingScreen
import io.justpilot.justeinbuergerung.ui.session.SessionScreen
import io.justpilot.justeinbuergerung.ui.settings.SettingsScreen
import kotlin.reflect.KClass

@Composable
fun JustApp(viewModel: AppViewModel = viewModel(factory = ViewModels.factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (state) {
        // Reading the settings takes a few milliseconds; show the plain background meanwhile.
        AppUiState.Loading -> Surface(Modifier.fillMaxSize()) {}
        AppUiState.Onboarding -> OnboardingScreen(onSelect = viewModel::selectFederalState)
        AppUiState.Ready -> MainNavigation()
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
                HomeScreen(onStartDaily = { navController.navigate(SessionRoute(SessionSelection.Daily.key)) })
            }
            composable<CatalogRoute> {
                CatalogScreen(onStartSession = { navController.navigate(SessionRoute(it.key)) })
            }
            composable<SettingsRoute> { SettingsScreen() }
            composable<SessionRoute> { SessionScreen(onClose = { navController.popBackStack() }) }
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
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
