package com.eduquiz.feature.profile

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.Decoder
import com.eduquiz.core.resources.resolveCosmeticOverlayModel
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.UserStats
import com.eduquiz.feature.auth.presentation.AuthViewModel

@Composable
fun ProfileFeature(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                addGifDecoderIfAvailable()
            }
            .build()
    }

    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val achievements by profileViewModel.achievements.collectAsStateWithLifecycle()
    val userStats by profileViewModel.userStats.collectAsStateWithLifecycle()

    val user = (authState as? com.eduquiz.feature.auth.model.AuthState.Authenticated)?.user
    
    LaunchedEffect(user?.uid) {
        user?.uid?.let { 
            profileViewModel.initialize(it)
        }
    }

    val blueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3B82F6),
            Color(0xFF1E3A8A)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(blueGradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileHeader(
                    coins = profile?.coins ?: 0,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            
            item {
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
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                UserStatsRow(
                    xp = userStats?.totalXp ?: 0L,
                    totalQuestions = userStats?.totalQuestions ?: 0,
                    totalAttempts = userStats?.totalAttempts ?: 0,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mis estadísticas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                PerformanceDonutChart(
                    correctAnswers = userStats?.totalCorrectAnswers ?: 0,
                    incorrectAnswers = userStats?.incorrectAnswers ?: 0,
                    totalQuestions = userStats?.totalQuestions ?: 0,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                EfficiencyGaugeChart(
                    efficiency = userStats?.efficiency ?: 0f,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                AchievementsComparisonChart(
                    totalScore = userStats?.totalScore ?: 0,
                    totalXp = userStats?.totalXp ?: 0L,
                    totalCorrectAnswers = userStats?.totalCorrectAnswers ?: 0,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tus logros",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            if (achievements.isEmpty()) {
                item {
                    Text(
                        text = "No hay logros desbloqueados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                items(achievements) { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    coins: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = coins.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                    Text(
                        text = "🪙",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ProfilePhotoSection(
    photoUrl: String?,
    displayName: String,
    cosmeticOverlayUrl: String?,
    selectedCosmeticId: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                addGifDecoderIfAvailable()
            }
            .build()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Foto de perfil de $displayName",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (selectedCosmeticId == "basic_frame") {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(
                            width = 4.dp,
                            color = Color(0xFF3B82F6),
                            shape = CircleShape
                        )
                )
            } else {
                cosmeticOverlayUrl?.let { overlayUrl ->
                    val overlayModel = resolveCosmeticOverlayModel(context, overlayUrl)
                    AsyncImage(
                        model = overlayModel,
                        imageLoader = gifImageLoader,
                        contentDescription = "Decoración de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun UserStatsRow(
    xp: Long,
    totalQuestions: Int,
    totalAttempts: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = "$xp Pt",
            label = "Puntos de\nExperiencia"
        )
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(50.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        StatItem(
            value = totalQuestions.toString(),
            label = "Preguntas\ntotales"
        )
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(50.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        StatItem(
            value = totalAttempts.toString(),
            label = "Intentos\ntotales"
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 28.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PerformanceDonutChart(
    correctAnswers: Int,
    incorrectAnswers: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gráfica de rendimiento general",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (totalQuestions > 0) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { correctAnswers.toFloat() / totalQuestions },
                        modifier = Modifier.size(200.dp),
                        color = Color(0xFF3B82F6),
                        strokeWidth = 30.dp,
                        trackColor = Color(0xFF93C5FD)
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalQuestions.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Preguntas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val correctPercentage = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
                    val incorrectPercentage = 100 - correctPercentage
                    
                    LegendItem(
                        color = Color(0xFF3B82F6),
                        label = "Correctas",
                        percentage = "$correctPercentage%"
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    LegendItem(
                        color = Color(0xFF93C5FD),
                        label = "Incorrectas",
                        percentage = "$incorrectPercentage%"
                    )
                }
            } else {
                Text(
                    text = "No hay datos disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1E3A8A)
        )
        Text(
            text = percentage,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun EfficiencyGaugeChart(
    efficiency: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gráfica de eficiencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { efficiency / 100f },
                    modifier = Modifier.size(180.dp),
                    color = when {
                        efficiency >= 75f -> Color(0xFF10B981)
                        efficiency >= 50f -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    strokeWidth = 24.dp,
                    trackColor = Color(0xFFE5E7EB)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${efficiency.toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "de aciertos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsComparisonChart(
    totalScore: Int,
    totalXp: Long,
    totalCorrectAnswers: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Gráfica comparativa de logros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (totalScore > 0 || totalXp > 0 || totalCorrectAnswers > 0) {
                val maxValue = maxOf(totalScore.toFloat(), totalXp.toFloat(), totalCorrectAnswers.toFloat())
                
                ComparisonBar(
                    label = "Total Score",
                    value = totalScore,
                    maxValue = maxValue,
                    color = Color(0xFF3B82F6)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ComparisonBar(
                    label = "Total XP",
                    value = totalXp.toInt(),
                    maxValue = maxValue,
                    color = Color(0xFF8B5CF6)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ComparisonBar(
                    label = "Respuestas Correctas",
                    value = totalCorrectAnswers,
                    maxValue = maxValue,
                    color = Color(0xFF10B981)
                )
            } else {
                Text(
                    text = "No hay datos disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonBar(
    label: String,
    value: Int,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E3A8A)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (maxValue > 0) value / maxValue else 0f)
                    .height(24.dp)
                    .background(color, RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val achievementInfo = getAchievementInfo(achievement.achievementId)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = achievementInfo.icon,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 40.sp
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = achievementInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = achievementInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

private fun getAchievementInfo(achievementId: String): AchievementInfo {
    return when (achievementId) {
        "first_exam" -> AchievementInfo(
            name = "Primer Simulacro",
            description = "Completa tu primer simulacro",
            icon = "🎯"
        )
        "streak_3_days" -> AchievementInfo(
            name = "3 Días de Racha",
            description = "Entra a la app 3 días seguidos",
            icon = "🔥"
        )
        "correct_answers_10" -> AchievementInfo(
            name = "10 Respuestas Correctas",
            description = "Acumula 10 respuestas correctas",
            icon = "⭐"
        )
        else -> AchievementInfo(
            name = achievementId,
            description = "Logro desbloqueado",
            icon = "🏆"
        )
    }
}

private data class AchievementInfo(
    val name: String,
    val description: String,
    val icon: String
)

private fun coil.ComponentRegistry.Builder.addGifDecoderIfAvailable() {
    val factory: Decoder.Factory? =
        if (Build.VERSION.SDK_INT >= 28) {
            tryNewDecoderFactory("coil.decode.ImageDecoderDecoder\$Factory")
                ?: tryNewDecoderFactory("coil.decode.GifDecoder\$Factory")
        } else {
            tryNewDecoderFactory("coil.decode.GifDecoder\$Factory")
        }

    if (factory != null) add(factory)
}

private fun tryNewDecoderFactory(className: String): Decoder.Factory? {
    return runCatching {
        val clazz = Class.forName(className)
        clazz.getDeclaredConstructor().newInstance() as Decoder.Factory
    }.getOrNull()
}
