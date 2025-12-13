package com.eduquiz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eduquiz.app.R

// Definir la paleta de colores para el tema claro
private val LightColors = lightColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7FAFF),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0E0FF),
    onTertiaryContainer = Color(0xFF001F52),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FAFF),
    onBackground = Color(0xFF1C1B1E),
    surface = Color(0xFFF7FAFF),
    onSurface = Color(0xFF1C1B1E),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    inverseOnSurface = Color(0xFFF4F0F4),
    inverseSurface = Color(0xFF313033),
    inversePrimary = Color(0xFFB0C6FF),
    surfaceTint = Color(0xFF3B82F6),
    outlineVariant = Color(0xFFCAC4CF),
    scrim = Color(0xFF000000)
)

// Definir la paleta de colores para el tema oscuro (ejemplo, ajustar según necesidad)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFD0E0FF),
    secondary = Color(0xFFB0C6FF),
    onSecondary = Color(0xFF002B75),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFD0E0FF),
    tertiary = Color(0xFFB0C6FF),
    onTertiary = Color(0xFF001F52),
    tertiaryContainer = Color(0xFF2563EB),
    onTertiaryContainer = Color(0xFFD0E0FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF1C1B1E),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1C1B1E),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    inverseOnSurface = Color(0xFF1C1B1E),
    inverseSurface = Color(0xFFE6E1E6),
    inversePrimary = Color(0xFF3B82F6),
    surfaceTint = Color(0xFF60A5FA),
    outlineVariant = Color(0xFF49454E),
    scrim = Color(0xFF000000)
)

@Composable
fun EduQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
