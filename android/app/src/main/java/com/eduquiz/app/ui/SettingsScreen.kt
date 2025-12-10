package com.eduquiz.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.feature.auth.presentation.AuthViewModel

@Composable
fun SettingsScreen(
    onNavigate: (route: String) -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    
    var notificationEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var soundEffectsEnabled by remember { mutableStateOf(true) }
    var vibrationsEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Español") }
    var expandedMusicSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E90FF))
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Header con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E90FF),
                            Color(0xFF1978E6)
                        )
                    )
                )
                .padding(24.dp)
                .padding(top = 8.dp)
        ) {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Opción: Notificación
            SettingsItem(
                icon = Icons.Default.NotificationsActive,
                title = "Notificación",
                iconBackgroundColor = Color(0xFFFF6B6B),
                content = {
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00CC44),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(width = 52.dp, height = 28.dp)
                    )
                }
            )

            // Opción: Música y efectos (expandible)
            var isExpanded by remember { mutableStateOf(expandedMusicSection) }
            SettingsExpandableItem(
                icon = Icons.Default.VolumeUp,
                title = "Música y efectos",
                iconBackgroundColor = Color(0xFF4D96FF),
                isExpanded = isExpanded,
                onToggleExpand = { 
                    isExpanded = !isExpanded
                    expandedMusicSection = isExpanded
                }
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFF1978E6).copy(alpha = 0.7f),
                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sub-opción: Música
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Música",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = musicEnabled,
                                onCheckedChange = { musicEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00CC44),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(width = 48.dp, height = 24.dp)
                            )
                        }

                        // Sub-opción: Efectos sonoros
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Efectos sonoros",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = { soundEffectsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00CC44),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(width = 48.dp, height = 24.dp)
                            )
                        }

                        // Sub-opción: Vibraciones
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vibraciones",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = vibrationsEnabled,
                                onCheckedChange = { vibrationsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00CC44),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(width = 48.dp, height = 24.dp)
                            )
                        }
                    }
                }
            }

            // Opción: Idioma
            SettingsLanguageItem(
                icon = Icons.Default.Language,
                title = "Idioma",
                selectedLanguage = selectedLanguage,
                iconBackgroundColor = Color(0xFF52B788),
                onLanguageSelected = { selectedLanguage = it }
            )

            // Opción: Acerca de EduQuiz
            SettingsClickableItem(
                icon = Icons.Default.Info,
                title = "Acerca de EduQuiz",
                onClick = { onNavigate("about") },
                iconBackgroundColor = Color(0xFF6366F1)
            )

            // Opción: Cerrar sesión
            SettingsClickableItem(
                icon = Icons.Default.Logout,
                title = "Cerrar sesión",
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                isDestructive = true,
                iconBackgroundColor = Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    iconBackgroundColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = iconBackgroundColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(28.dp),
                            tint = iconBackgroundColor
                        )
                    }
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsExpandableItem(
    icon: ImageVector,
    title: String,
    iconBackgroundColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = iconBackgroundColor.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(28.dp),
                                tint = iconBackgroundColor
                            )
                        }
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            expandedContent()
        }
    }
}

@Composable
private fun SettingsLanguageItem(
    icon: ImageVector,
    title: String,
    selectedLanguage: String,
    iconBackgroundColor: Color,
    onLanguageSelected: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val languages = listOf("Español", "English", "Français", "Deutsch")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = iconBackgroundColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(28.dp),
                            tint = iconBackgroundColor
                        )
                    }
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            // Dropdown selector
            Box {
                Surface(
                    modifier = Modifier
                        .clickable { isDropdownExpanded = !isDropdownExpanded }
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = selectedLanguage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Cambiar idioma",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.95f))
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = language,
                                    color = if (language == selectedLanguage) Color(0xFF1E90FF) else Color.Black,
                                    fontWeight = if (language == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onLanguageSelected(language)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    iconBackgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) 
                Color(0xFFEF4444).copy(alpha = 0.15f) 
            else 
                Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = iconBackgroundColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(28.dp),
                            tint = iconBackgroundColor
                        )
                    }
                }
                Text(
                    text = title,
                    color = if (isDestructive) Color(0xFFEF4444) else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir a",
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive) Color(0xFFEF4444) else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
