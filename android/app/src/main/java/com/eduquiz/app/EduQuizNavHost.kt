package com.eduquiz.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.app.navigation.RootDestination
import com.eduquiz.app.ui.AboutScreen
import com.eduquiz.app.ui.HomeScreen
import com.eduquiz.app.ui.NotificationsScreen
import com.eduquiz.app.ui.SettingsScreen
import com.eduquiz.app.ui.theme.EduQuizTheme
import com.eduquiz.data.repository.OnboardingRepository
import com.eduquiz.feature.auth.model.AuthUser
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.presentation.AuthViewModel
import com.eduquiz.feature.auth.ui.LoginRoute
import com.eduquiz.feature.auth.ui.OnboardingRoute
import com.eduquiz.feature.exam.ExamFeature
import com.eduquiz.feature.profile.ProfileFeature
import com.eduquiz.feature.pack.PackFeature
import com.eduquiz.feature.ranking.RankingFeature
import com.eduquiz.feature.store.StoreFeature

@Composable
fun EduQuizNavHost(
    onboardingRepository: OnboardingRepository
) {
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
                else -> {
                    // Check if onboarding has been completed
                    val hasCompletedOnboarding by onboardingRepository.hasCompletedOnboarding.collectAsStateWithLifecycle(initialValue = false)
                    
                    if (hasCompletedOnboarding) {
                        LoginRoute(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = authViewModel
                        )
                    } else {
                        OnboardingRoute(
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToLogin = {
                                // After onboarding, user will see login screen on next auth state change
                            }
                        )
                    }
                }
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
        BottomNavItem(RootDestination.Store.route, "Tienda", Icons.Default.ShoppingCart),
        BottomNavItem(RootDestination.Ranking.route, "Tabla de\nclasificacion", Icons.Default.Star),
        BottomNavItem(RootDestination.Settings.route, "Ajustes", Icons.Default.Settings)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentRoute != RootDestination.Auth.route && currentRoute != RootDestination.Pack.route && currentRoute != RootDestination.Exam.route && currentRoute != RootDestination.Notifications.route && currentRoute != RootDestination.About.route) {
                EduQuizBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
            // MERGE: Usamos StoreFeature con modifier explícito para consistencia
            composable(RootDestination.Store.route) {
                StoreFeature(
                    modifier = Modifier.fillMaxSize()
                )
            }
            // MERGE: Usamos la versión con 'uid' (de monica) dentro de la estructura nueva
            composable(RootDestination.Ranking.route) {
                RankingFeature(
                    uid = authUser.uid,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(RootDestination.Settings.route) {
                SettingsScreen(
                    onNavigate = { route ->
                        when (route) {
                            "about" -> navController.navigate(RootDestination.About.route)
                            else -> {}
                        }
                    },
                    onLogout = {
                        navController.popBackStack(RootDestination.Home.route, inclusive = true)
                        onLogout()
                    }
                )
            }
            composable(RootDestination.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(RootDestination.Notifications.route) {
                val homeProfileViewModel: com.eduquiz.app.ui.HomeProfileViewModel = hiltViewModel()
                val notificationsEnabled by homeProfileViewModel.notificationsEnabled.collectAsStateWithLifecycle()
                val context = androidx.compose.ui.platform.LocalContext.current

                androidx.compose.runtime.LaunchedEffect(notificationsEnabled) {
                    if (!notificationsEnabled) {
                        android.widget.Toast.makeText(
                            context,
                            "Notificaciones desactivadas en ajustes",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                    }
                }

                if (notificationsEnabled) {
                    NotificationsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            // MERGE: Lista de exclusión actualizada para incluir Settings, About y Notifications
            RootDestination.allDestinations
                .filter { it !in setOf(RootDestination.Home, RootDestination.Auth, RootDestination.Profile, RootDestination.Pack, RootDestination.Exam, RootDestination.Store, RootDestination.Ranking, RootDestination.Settings, RootDestination.About, RootDestination.Notifications) }
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
private fun EduQuizBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                EduQuizBottomNavItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun EduQuizBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) Color.Black else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = if (isSelected) Color.Black else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

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
