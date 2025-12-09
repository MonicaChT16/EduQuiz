package com.eduquiz.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E90FF)) // Azul principal como en la imagen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header con perfil, XP, monedas y notificaciones
            HomeHeader(
                authState = authState,
                onNotificationClick = { onNavigate(RootDestination.Notifications) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Título PISA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Evaluación Internacional de Estudiantes",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PISA",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cuerpo principal con GIF del robot y botón tienda
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // GIF del robot
                AsyncImage(
                    model = R.drawable.robot,
                    contentDescription = "Robot PISA animado",
                    imageLoader = imageLoader, // Usar el ImageLoader personalizado
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                // Botón Tienda - fijo en la esquina superior derecha
                Button(
                    onClick = { onNavigate(RootDestination.Store) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(80.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Recuadro PISA con información y botón Jugar ahora
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A5F8F).copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                        onClick = { onNavigate(RootDestination.Exam) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
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
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Gray
                    )
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
                    imageVector = Icons.Default.ShoppingCart,
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
                    imageVector = Icons.Default.ShoppingCart,
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
