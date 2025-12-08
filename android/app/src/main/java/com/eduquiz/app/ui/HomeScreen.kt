package com.eduquiz.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eduquiz.app.navigation.RootDestination

@Composable
fun HomeScreen(
    onNavigate: (RootDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "EduQuiz",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Explora las áreas principales de la plataforma.",
            style = MaterialTheme.typography.bodyLarge
        )
        RootDestination.allDestinations
            .filter { it != RootDestination.Home }
            .forEach { destination ->
                Button(onClick = { onNavigate(destination) }) {
                    Text(text = destination.title)
                }
            }
    }
}
