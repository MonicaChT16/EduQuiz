package com.eduquiz.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage // Importación de Coil
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.eduquiz.app.R // Importación para acceder a los recursos
import com.eduquiz.app.navigation.RootDestination
import com.eduquiz.feature.auth.presentation.AuthViewModel
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.domain.pack.Subject
import com.eduquiz.feature.exam.ExamNavigationHelper

@Composable
fun HomeScreen(
    onNavigate: (RootDestination) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    var showSubjectDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    if (showSubjectDialog) {
        SubjectSelectionDialog(
            selectedSubject = selectedSubject,
            onSubjectSelected = { selectedSubject = it },
            onStart = {
                val subjectCode = selectedSubject?.let { getSubjectCodeFromDisplayName(it) }
                if (subjectCode != null) {
                    android.util.Log.d("HomeScreen", "Navigating to exam with subject: $subjectCode")
                    // Guardar la materia para que ExamFeature la use
                    ExamNavigationHelper.setPendingSubject(subjectCode)
                    showSubjectDialog = false
                    // Navegar al examen - ExamFeature iniciará automáticamente el examen con la materia
                    onNavigate(RootDestination.Exam)
                } else {
                    android.util.Log.e("HomeScreen", "Could not convert subject name to code: $selectedSubject")
                }
            },
            onDismiss = { showSubjectDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E90FF)) // Azul principal como en la imagen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con perfil, XP, monedas y notificaciones
            HomeHeader(
                authState = authState,
                onNotificationClick = { onNavigate(RootDestination.Notifications) },
                modifier = Modifier
                    .fillMaxWidth()
            )

            // Título PISA
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Evaluación Internacional de Estudiantes",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PISA",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }


            // Cuerpo principal con GIF del robot y botón tienda
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // GIF del robot
                AsyncImage(
                    model = R.drawable.robot,
                    contentDescription = "Robot PISA animado",
                    imageLoader = imageLoader, // Usar el ImageLoader personalizado
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                // Botón Tienda - fijo en la esquina superior derecha
                Button(
                    onClick = { onNavigate(RootDestination.Store) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(80.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A5F8F).copy(alpha = 0.9f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Tienda",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "Tienda",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }


            // Recuadro PISA con información y botón Jugar ahora
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A5F8F).copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Preparación Prueba PISA",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Descubre cuestionarios diseñados especialmente para tu éxito en el examen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showSubjectDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00CC44) // Verde brillante como en la imagen
                        )
                    ) {
                        Text(
                            text = "Jugar ahora",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectSelectionDialog(
    selectedSubject: String?,
    onSubjectSelected: (String) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        SubjectOption(
            "Matemáticas",
            Icons.Default.School,
            Color(0xFF4CAF50),
            "¡Afila tu ingenio! Resuelve problemas y domina los números."
        ),
        SubjectOption(
            "Comprensión lectora",
            Icons.Default.MenuBook,
            Color(0xFF3F51B5),
            "Desentraña el significado oculto. Lee, interpreta y comprende textos."
        ),
        SubjectOption(
            "Ciencias",
            Icons.Default.Science,
            Color(0xFF009688),
            "Explora el mundo que te rodea. Descubre los secretos de la naturaleza y el universo."
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona la siguiente materia:",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3050)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    options.forEach { option ->
                        SubjectOptionCard(
                            option = option,
                            isSelected = selectedSubject == option.title,
                            onClick = { onSubjectSelected(option.title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = selectedSubject != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CC44))
                ) {
                    Text(
                        text = "Iniciar intento",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectOptionCard(
    option: SubjectOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background = if (isSelected) Color(0xFF1E90FF).copy(alpha = 0.12f) else Color(0xFFF6F8FF)
    val borderColor = if (isSelected) Color(0xFF1E90FF) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(14.dp),
                color = option.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = option.color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C6476)
                )
            }
        }
    }
}

private data class SubjectOption(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
)

/**
 * Convierte el nombre de la materia (ej: "Matemáticas") al código (ej: Subject.MATEMATICA)
 */
private fun getSubjectCodeFromDisplayName(displayName: String): String? {
    return when (displayName) {
        "Matemáticas" -> Subject.MATEMATICA
        "Comprensión lectora" -> Subject.COMPRENSION_LECTORA
        "Ciencias" -> Subject.CIENCIAS
        else -> {
            // Intentar buscar por coincidencia parcial
            when {
                displayName.contains("Matemática", ignoreCase = true) -> Subject.MATEMATICA
                displayName.contains("Comprensión", ignoreCase = true) || 
                displayName.contains("lectora", ignoreCase = true) -> Subject.COMPRENSION_LECTORA
                displayName.contains("Ciencia", ignoreCase = true) -> Subject.CIENCIAS
                else -> null
            }
        }
    }
}

@Composable
fun HomeHeader(
    authState: AuthState,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: HomeProfileViewModel = hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Perfil y nombre
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    val photoUrl = profile?.photoUrl
                    if (photoUrl != null) {
                        // Mostrar la imagen del perfil si existe
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil del usuario",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Mostrar icono por defecto si no hay foto
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val userName = when (authState) {
                    is AuthState.Authenticated -> authState.user.displayName ?: "Usuario"
                    else -> "Usuario"
                }
                Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // XP
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "XP",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFFFD700)
                )
                Text(
                    text = "${profile?.coins ?: 0} XP",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Monedas
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Monedas",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFFFD700)
                )
                Text(
                    text = "${profile?.coins ?: 0}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón Notificaciones
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier.size(48.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}
