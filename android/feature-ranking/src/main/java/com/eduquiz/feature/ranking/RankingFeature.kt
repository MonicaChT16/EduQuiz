package com.eduquiz.feature.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.domain.ranking.LeaderboardEntry

@Composable
fun RankingFeature(
    uid: String,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel()
) {
    LaunchedEffect(uid) {
        viewModel.start(uid)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize()) {
        when {
            state.error != null -> ErrorState(message = state.error!!)
            state.isLoading -> LoadingState()
            else -> RankingList(
                title = state.classroomLabel ?: "Tu aula",
                entries = state.entries,
                currentUid = state.currentUid
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Cargando ranking…")
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No pudimos cargar el ranking", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RankingList(
    title: String,
    entries: List<LeaderboardEntry>,
    currentUid: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ranking del aula",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                RankingRow(
                    position = index + 1,
                    entry = entry,
                    isCurrentUser = entry.uid == currentUid
                )
            }
        }
    }
}

@Composable
private fun RankingRow(
    position: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#$position",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column {
                Text(
                    text = entry.displayName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = entry.uid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${entry.totalScore} pts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
        )
    }
}
