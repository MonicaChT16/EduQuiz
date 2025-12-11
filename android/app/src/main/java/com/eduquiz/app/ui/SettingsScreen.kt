package com.eduquiz.app.ui

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
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
    var selectedLanguage by remember { mutableStateOf("Español") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Azul más oscuro arriba
                        Color(0xFF3B82F6)  // Azul más claro abajo
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // Settings Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notificación
                ModernSettingsItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notificación",
                    iconColor = Color(0xFFFF6B6B),
                    content = {
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { notificationEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF3B82F6),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                    }
                )

                // Música y efectos
                ModernSettingsItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Música y efectos",
                    iconColor = Color(0xFF8B5CF6),
                    content = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Ir a",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { /* TODO: Navigate to music settings */ }
                )

                // Idioma
                ModernSettingsItem(
                    icon = Icons.Default.Language,
                    title = "Idioma",
                    iconColor = Color(0xFF10B981),
                    content = {
                        ModernLanguageSelector(
                            selectedLanguage = selectedLanguage,
                            onLanguageSelected = { selectedLanguage = it }
                        )
                    }
                )

                // Acerca de Quizzie Bot
                ModernSettingsItem(
                    icon = Icons.Default.Info,
                    title = "Acerca de Quizzie Bot",
                    iconColor = Color(0xFF6366F1),
                    onClick = { onNavigate("about") }
                )

                // Cerrar sesión
                ModernSettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Cerrar sesión",
                    iconColor = Color(0xFFEF4444),
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = "Privacy Policy @ Quizzie Bot",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Navigation
        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedTab = 3, // Settings tab
            onTabSelected = { /* TODO: Handle navigation */ }
        )
    }
}

@Composable
private fun ModernSettingsItem(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    content: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono circular con fondo blanco
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        content?.invoke()
    }
}

@Composable
private fun ModernLanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val languages = listOf("Español", "English", "Français", "Deutsch")

    Box {
        Surface(
            modifier = Modifier
                .clickable { isDropdownExpanded = !isDropdownExpanded }
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.Transparent
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selectedLanguage,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Cambiar idioma",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
            modifier = Modifier.background(
                Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language,
                            color = if (language == selectedLanguage) Color(0xFF3B82F6) else Color.Black,
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

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Inicio",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Perfil",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = Icons.Default.EmojiEvents,
                label = "Tabla de\nclasificación",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Ajustes",
                isSelected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
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
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.Black else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
