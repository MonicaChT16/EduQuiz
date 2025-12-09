package com.eduquiz.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.app.navigation.RootDestination
import com.eduquiz.app.ui.HomeScreen
import com.eduquiz.app.ui.NotificationsScreen
import com.eduquiz.app.ui.SettingsScreen
import com.eduquiz.app.ui.theme.EduQuizTheme
import com.eduquiz.feature.auth.model.AuthUser
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.presentation.AuthViewModel
import com.eduquiz.feature.auth.ui.LoginRoute
import com.eduquiz.feature.exam.ExamFeature
import com.eduquiz.feature.profile.ProfileFeature
import com.eduquiz.feature.pack.PackFeature
import com.eduquiz.feature.ranking.RankingFeature
import com.eduquiz.feature.store.StoreFeature

@Composable
fun EduQuizNavHost() {
    EduQuizTheme {
        val authViewModel: AuthViewModel = hiltViewModel()
        val authState by authViewModel.state.collectAsStateWithLifecycle()

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (authState) {
                AuthState.Loading -> LoadingScreen()
                is AuthState.Authenticated -> {
                    val authUser = (authState as AuthState.Authenticated).user
                    MainNavHost(
                        authUser = authUser,
                        modifier = Modifier.fillMaxSize(),
                        onLogout = { authViewModel.logout() }
                    )
                }
                else -> LoginRoute(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = authViewModel
                )
            }
        }
    }
}

@Composable
private fun MainNavHost(authUser: AuthUser, modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(RootDestination.Home.route, "Inicio", Icons.Default.Home),
        BottomNavItem(RootDestination.Profile.route, "Perfil", Icons.Default.Person),
        BottomNavItem(RootDestination.Ranking.route, "Tabla de clasificación", Icons.Default.Star),
        BottomNavItem(RootDestination.Settings.route, "Ajustes", Icons.Default.Settings)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentRoute != RootDestination.Auth.route && currentRoute != RootDestination.Pack.route && currentRoute != RootDestination.Exam.route && currentRoute != RootDestination.Notifications.route) {
                BottomAppBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Evita múltiples copias de la misma pantalla en la pila
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    // Evita múltiples copias de la misma pantalla cuando se selecciona el mismo elemento
                                    launchSingleTop = true
                                    // Restaura el estado cuando se vuelve a seleccionar el elemento
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RootDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RootDestination.Home.route) {
                HomeScreen(
                    onNavigate = { destination ->
                        if (destination != RootDestination.Home && destination != RootDestination.Auth) {
                            navController.navigate(destination.route)
                        }
                    }
                )
            }
            composable(RootDestination.Profile.route) {
                ProfileFeature(onLogoutClick = onLogout)
            }
            composable(RootDestination.Pack.route) {
                PackFeature(
                    modifier = Modifier.fillMaxSize(),
                    onStartExam = { navController.navigate(RootDestination.Exam.route) }
                )
            }
            composable(RootDestination.Exam.route) {
                ExamFeature(
                    uid = authUser.uid,
                    modifier = Modifier.fillMaxSize(),
                    onExit = {
                        navController.popBackStack(RootDestination.Home.route, inclusive = false)
                    }
                )
            }
            composable(RootDestination.Store.route) {
                StoreFeature(
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(RootDestination.Ranking.route) {
                RankingFeature()
            }
            composable(RootDestination.Settings.route) {
                SettingsScreen()
            }
            composable(RootDestination.Notifications.route) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            RootDestination.allDestinations
                .filter { it !in setOf(RootDestination.Home, RootDestination.Auth, RootDestination.Profile, RootDestination.Pack, RootDestination.Exam, RootDestination.Store, RootDestination.Ranking, RootDestination.Settings, RootDestination.Notifications) }
                .forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(label = destination.title)
                    }
                }
        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)

@Composable
private fun PlaceholderScreen(label: String, modifier: Modifier = Modifier) {
    Text(
        text = "$label coming soon",
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
