package com.eduquiz.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.feature.auth.presentation.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileFeature(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val attempts by profileViewModel.attempts.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()

    val user = (authState as? com.eduquiz.feature.auth.model.AuthState.Authenticated)?.user
    
    // Inicializar ViewModel con el UID del usuario
    LaunchedEffect(user?.uid) {
        user?.uid?.let { profileViewModel.initialize(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium
        )
        if (user != null) {
            Text(
                text = "Usuario: ${user.displayName}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Mostrar coins
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EduCoins",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${profile?.coins ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Mostrar cosmetic seleccionado
        profile?.selectedCosmeticId?.let { cosmeticId ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cosmético Equipado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cosmeticId,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Button(onClick = onLogoutClick) {
            Text(text = "Cerrar sesion")
        }
        
        Text(
            text = "Historial de intentos",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        if (attempts.isEmpty()) {
            Text(
                text = "No hay intentos registrados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attempts) { attempt ->
                    AttemptCard(attempt = attempt)
                }
            }
        }
    }
}

@Composable
private fun AttemptCard(attempt: ExamAttempt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(attempt.startedAtLocal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getStatusLabel(attempt.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = getStatusColor(attempt.status)
                )
            }
            Text(
                text = "Pack: ${attempt.packId}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Puntaje: ${attempt.scoreRaw}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            attempt.finishedAtLocal?.let { finishedAt ->
                Text(
                    text = "Finalizado: ${formatDate(finishedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun getStatusLabel(status: String): String {
    return when (status) {
        ExamStatus.COMPLETED -> "Completado"
        ExamStatus.AUTO_SUBMIT -> "Auto-enviado"
        ExamStatus.CANCELLED_CHEAT -> "Cancelado"
        ExamStatus.IN_PROGRESS -> "En progreso"
        else -> status
    }
}

@Composable
private fun getStatusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status) {
        ExamStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        ExamStatus.AUTO_SUBMIT -> MaterialTheme.colorScheme.secondary
        ExamStatus.CANCELLED_CHEAT -> MaterialTheme.colorScheme.error
        ExamStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
