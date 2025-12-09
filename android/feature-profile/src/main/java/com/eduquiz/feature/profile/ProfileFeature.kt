package com.eduquiz.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.profile.Achievement
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
    val achievements by profileViewModel.achievements.collectAsStateWithLifecycle()
    val uploadError by profileViewModel.uploadError.collectAsStateWithLifecycle()
    val isUploading by profileViewModel.isUploading.collectAsStateWithLifecycle()

    val user = (authState as? com.eduquiz.feature.auth.model.AuthState.Authenticated)?.user
    
    // Inicializar ViewModel con el UID del usuario
    LaunchedEffect(user?.uid) {
        user?.uid?.let { 
            profileViewModel.initialize(it)
        }
    }

    // Launcher para seleccionar imagen de la galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileViewModel.uploadProfilePhoto(it)
        }
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
        
        // Foto de perfil con overlay del cosmético
        var cosmeticOverlayUrl: String? by remember { mutableStateOf(null) }
        
        LaunchedEffect(profile?.selectedCosmeticId) {
            profile?.selectedCosmeticId?.let { cosmeticId ->
                cosmeticOverlayUrl = profileViewModel.getCosmeticOverlayUrl(cosmeticId)
            } ?: run {
                cosmeticOverlayUrl = null
            }
        }
        
        ProfilePhotoSection(
            photoUrl = profile?.photoUrl ?: user?.photoUrl,
            displayName = user?.displayName ?: "Usuario",
            cosmeticOverlayUrl = cosmeticOverlayUrl,
            selectedCosmeticId = profile?.selectedCosmeticId,
            isUploading = isUploading,
            onPhotoClick = {
                if (!isUploading) {
                    imagePickerLauncher.launch("image/*")
                }
            }
        )

        // Mostrar error de subida si existe
        uploadError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
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
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { profileViewModel.clearUploadError() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
        
        if (user != null) {
            Text(
                text = "Usuario: ${user.displayName ?: "Usuario"}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Mostrar coins y XP
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "XP (Experiencia)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${profile?.xp ?: 0L}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
        
        // Sección de logros
        Text(
            text = "Logros",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        if (achievements.isEmpty()) {
            Text(
                text = "No hay logros desbloqueados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            achievements.forEach { achievement ->
                AchievementCard(achievement = achievement)
            }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
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

@Composable
private fun AchievementCard(achievement: Achievement) {
    val achievementInfo = getAchievementInfo(achievement.achievementId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = achievementInfo.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = achievementInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Desbloqueado: ${formatDate(achievement.unlockedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getAchievementInfo(achievementId: String): AchievementInfo {
    return when (achievementId) {
        "first_exam" -> AchievementInfo(
            name = "Primer Simulacro",
            description = "Completa tu primer simulacro"
        )
        "streak_3_days" -> AchievementInfo(
            name = "3 Días de Racha",
            description = "Entra a la app 3 días seguidos"
        )
        "correct_answers_10" -> AchievementInfo(
            name = "10 Respuestas Correctas",
            description = "Acumula 10 respuestas correctas"
        )
        else -> AchievementInfo(
            name = achievementId,
            description = "Logro desbloqueado"
        )
    }
}

private data class AchievementInfo(
    val name: String,
    val description: String
)

@Composable
private fun ProfilePhotoSection(
    photoUrl: String?,
    displayName: String,
    cosmeticOverlayUrl: String?,
    selectedCosmeticId: String?,
    isUploading: Boolean = false,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contenedor para la foto con overlay
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isUploading, onClick = onPhotoClick),
                contentAlignment = Alignment.Center
            ) {
                // Indicador de carga
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Foto de perfil (capa base)
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil de $displayName",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder circular con iniciales
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Overlay del cosmético (capa superior)
                // Si el cosmético es "basic_frame", mostrar borde simple
                if (selectedCosmeticId == "basic_frame") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                } else {
                    cosmeticOverlayUrl?.let { overlayUrl ->
                        AsyncImage(
                            model = overlayUrl,
                            contentDescription = "Decoración de perfil",
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            Button(
                onClick = onPhotoClick,
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = if (photoUrl != null) "Cambiar foto" else "Subir foto")
                }
            }
        }
    }
}

