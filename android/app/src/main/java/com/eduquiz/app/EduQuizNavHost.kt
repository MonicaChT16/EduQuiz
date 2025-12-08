package com.eduquiz.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.app.navigation.RootDestination
import com.eduquiz.app.ui.HomeScreen
import com.eduquiz.app.ui.theme.EduQuizTheme
import com.eduquiz.feature.auth.model.AuthUser
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.presentation.AuthViewModel
import com.eduquiz.feature.auth.ui.LoginRoute
import com.eduquiz.feature.exam.ExamFeature
import com.eduquiz.feature.profile.ProfileFeature
import com.eduquiz.feature.pack.PackFeature

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
private fun MainNavHost(
    authUser: AuthUser,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RootDestination.Home.route,
        modifier = modifier
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
        RootDestination.allDestinations
            .filter { it !in setOf(RootDestination.Home, RootDestination.Auth, RootDestination.Profile, RootDestination.Pack, RootDestination.Exam) }
            .forEach { destination ->
                composable(destination.route) {
                    PlaceholderScreen(label = destination.title)
                }
            }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Text(
        text = "$label coming soon",
        style = MaterialTheme.typography.titleLarge
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
