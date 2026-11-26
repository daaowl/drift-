package app.sukoon.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.sukoon.R
import app.sukoon.core.designsystem.component.LocalNavAnimatedScope
import app.sukoon.core.designsystem.component.LocalSharedTransitionScope
import app.sukoon.domain.model.LaunchDestination
import app.sukoon.domain.model.UserSettings
import app.sukoon.ui.about.AboutScreen
import app.sukoon.ui.about.DocumentScreen
import app.sukoon.ui.counter.CounterScreen
import app.sukoon.ui.home.HomeScreen
import app.sukoon.ui.navigation.AboutRoute
import app.sukoon.ui.navigation.CollectionRoute
import app.sukoon.ui.navigation.CounterRoute
import app.sukoon.ui.navigation.DocumentRoute
import app.sukoon.ui.navigation.HomeRoute
import app.sukoon.ui.navigation.OnboardingRoute
import app.sukoon.ui.navigation.PinSetupRoute
import app.sukoon.ui.navigation.ReaderRoute
import app.sukoon.ui.navigation.SavedRoute
import app.sukoon.ui.navigation.SearchRoute
import app.sukoon.ui.navigation.SectionRoute
import app.sukoon.ui.navigation.SettingsRoute
import app.sukoon.ui.onboarding.OnboardingScreen
import app.sukoon.ui.reader.ReaderScreen
import app.sukoon.ui.saved.CollectionScreen
import app.sukoon.ui.saved.SavedScreen
import app.sukoon.ui.search.SearchScreen
import app.sukoon.ui.section.SectionScreen
import app.sukoon.ui.settings.PinSetupScreen
import app.sukoon.ui.settings.SettingsScreen
import kotlin.reflect.KClass

/** App-wide UI environment that screens read instead of threading parameters everywhere. */
data class AppEnvironment(
    val settings: UserSettings,
    val widthClass: WindowWidthSizeClass,
    val snackbar: SnackbarHostState,
)

val LocalAppEnvironment = staticCompositionLocalOf<AppEnvironment> { error("AppEnvironment not provided") }

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val topLevels = listOf(
    TopLevel(HomeRoute, HomeRoute::class, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    TopLevel(SearchRoute, SearchRoute::class, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    TopLevel(SavedRoute, SavedRoute::class, R.string.nav_saved, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    TopLevel(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun SukoonAppUi(
    settings: UserSettings,
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel,
) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val isTopLevel = topLevels.any { t -> destination?.hierarchy?.any { it.hasRoute(t.routeClass) } == true }
    val widthClass = windowSizeClass.widthSizeClass
    val useRail = widthClass != WindowWidthSizeClass.Compact

    val start: Any = remember {
        when {
            !settings.onboardingDone -> OnboardingRoute
            settings.launchDestination == LaunchDestination.FAVORITES -> SavedRoute
            else -> HomeRoute
        }
    }

    // Cold-start: optionally reopen the last dua.
    LaunchedEffect(Unit) {
        if (!mainViewModel.launchRoutingDone) {
            mainViewModel.launchRoutingDone = true
            val last = settings.lastReadDuaId
            if (settings.onboardingDone && settings.launchDestination == LaunchDestination.LAST_READ && last != null &&
                mainViewModel.pendingDuaId.value == null
            ) {
                nav.navigate(ReaderRoute(last))
            }
        }
    }

    // Notification / widget deep links.
    val pending by mainViewModel.pendingDuaId.collectAsStateWithLifecycle()
    LaunchedEffect(pending, settings.onboardingDone) {
        val id = pending ?: return@LaunchedEffect
        if (!settings.onboardingDone) return@LaunchedEffect
        nav.navigate(ReaderRoute(id)) { launchSingleTop = true }
        mainViewModel.consumePendingDua()
    }

    val env = AppEnvironment(settings, widthClass, snackbar)

    CompositionLocalProvider(LocalAppEnvironment provides env) {
        Row(Modifier.fillMaxSize()) {
            if (useRail && isTopLevel) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    topLevels.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { nav.navigateTopLevel(item.route) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (!useRail && isTopLevel) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            topLevels.forEach { item ->
                                val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { nav.navigateTopLevel(item.route) },
                                    icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                                    label = { Text(stringResource(item.label)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding),
                ) {
                    SukoonNavHost(nav, start, mainViewModel)
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun SukoonNavHost(nav: NavHostController, start: Any, mainViewModel: MainViewModel) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = nav,
                startDestination = start,
                enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = 0.98f, animationSpec = tween(260)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180)) },
            ) {
                composable<OnboardingRoute> {
                    Scoped {
                        OnboardingScreen(onFinished = {
                            mainViewModel.completeOnboarding()
                            nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                        })
                    }
                }
                composable<HomeRoute> {
                    Scoped {
                        HomeScreen(
                            onOpenSection = { nav.navigate(SectionRoute(it)) },
                            onOpenDua = { nav.navigate(ReaderRoute(it)) },
                            onSearch = { nav.navigateTopLevel(SearchRoute) },
                        )
                    }
                }
                composable<SearchRoute> {
                    Scoped { SearchScreen(onOpenDua = { nav.navigate(ReaderRoute(it)) }) }
                }
                composable<SavedRoute> {
                    Scoped {
                        SavedScreen(
                            onOpenFavorite = { nav.navigate(ReaderRoute(it, ReaderRoute.FAVORITES)) },
                            onOpenCollection = { nav.navigate(CollectionRoute(it)) },
                            onBrowse = { nav.navigateTopLevel(HomeRoute) },
                        )
                    }
                }
                composable<SettingsRoute> {
                    Scoped {
                        SettingsScreen(
                            onSetupPin = { nav.navigate(PinSetupRoute) },
                            onAbout = { nav.navigate(AboutRoute) },
                        )
                    }
                }
                composable<SectionRoute> {
                    Scoped {
                        SectionScreen(
                            onBack = { nav.popBackStack() },
                            onOpenDua = { duaId, sectionId -> nav.navigate(ReaderRoute(duaId, ReaderRoute.SECTION, sectionId)) },
                            onOpenCounter = { nav.navigate(CounterRoute(it)) },
                        )
                    }
                }
                composable<ReaderRoute>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(320)) + fadeIn(tween(220)) },
                    popExitTransition = { slideOutVertically(tween(260)) { it / 6 } + fadeOut(tween(200)) },
                ) {
                    Scoped {
                        ReaderScreen(
                            onBack = { nav.popBackStack() },
                            onOpenCounter = { nav.navigate(CounterRoute(it)) },
                        )
                    }
                }
                composable<CounterRoute>(
                    enterTransition = { slideInVertically(tween(320)) { it / 4 } + fadeIn(tween(220)) },
                    popExitTransition = { slideOutVertically(tween(260)) { it / 4 } + fadeOut(tween(200)) },
                ) {
                    Scoped { CounterScreen(onBack = { nav.popBackStack() }) }
                }
                composable<CollectionRoute> { entry ->
                    val route = entry.toRoute<CollectionRoute>()
                    Scoped {
                        CollectionScreen(
                            onBack = { nav.popBackStack() },
                            onOpenDua = { nav.navigate(ReaderRoute(it, ReaderRoute.COLLECTION, route.collectionId.toString())) },
                            onBrowse = { nav.navigateTopLevel(HomeRoute) },
                        )
                    }
                }
                composable<PinSetupRoute> {
                    Scoped { PinSetupScreen(onDone = { nav.popBackStack() }) }
                }
                composable<AboutRoute> {
                    Scoped {
                        AboutScreen(
                            onBack = { nav.popBackStack() },
                            onOpenDocument = { nav.navigate(DocumentRoute(it)) },
                        )
                    }
                }
                composable<DocumentRoute> { entry ->
                    val route = entry.toRoute<DocumentRoute>()
                    Scoped { DocumentScreen(document = route.document, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}

/** Exposes the destination's AnimatedVisibilityScope to shared-element modifiers. */
@Composable
private fun androidx.compose.animation.AnimatedVisibilityScope.Scoped(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides this) { content() }
}
