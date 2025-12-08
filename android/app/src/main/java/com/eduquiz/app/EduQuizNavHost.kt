package com.eduquiz.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eduquiz.app.navigation.RootDestination
import com.eduquiz.app.ui.HomeScreen
import com.eduquiz.app.ui.theme.EduQuizTheme

@Composable
fun EduQuizApp() {
    EduQuizTheme {
        val navController = rememberNavController()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController,
                startDestination = RootDestination.Home.route
            ) {
                composable(RootDestination.Home.route) {
                    HomeScreen(
                        onNavigate = { destination ->
                            if (destination != RootDestination.Home) {
                                navController.navigate(destination.route)
                            }
                        }
                    )
                }
                RootDestination.allDestinations.filter { it != RootDestination.Home }.forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(label = destination.title)
                    }
                }
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
