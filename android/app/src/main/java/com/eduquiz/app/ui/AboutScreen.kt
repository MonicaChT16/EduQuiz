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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E90FF))
    ) {
        // Header con botón de retroceso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            Text(
                text = "Acerca de EduQuiz",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }

        // Contenido principal con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Título de la aplicación
            Text(
                text = "EduQuiz Bot",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Plataforma de Educación Interactiva",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción general
            Text(
                text = "EduQuiz Bot es una aplicación innovadora diseñada para ayudarte a prepararte para la evaluación PISA y mejorar tus habilidades académicas a través de cuestionarios interactivos y gamificación.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de creadores
            Text(
                text = "Equipo de Desarrollo",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            // Creador 1
            CreatorCard(
                name = "Desarrollador Principal",
                role = "Arquitectura y Backend",
                description = "Responsable de la estructura general de la aplicación y el desarrollo de servicios."
            )

            // Creador 2
            CreatorCard(
                name = "Diseñador UI/UX",
                role = "Interfaz de Usuario",
                description = "Diseño visual y experiencia de usuario de la aplicación móvil."
            )

            // Creador 3
            CreatorCard(
                name = "Desarrollador Frontend",
                role = "Interfaz Móvil",
                description = "Desarrollo de la interfaz de usuario en Jetpack Compose."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Información adicional
            Text(
                text = "Versión 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "© 2024 EduQuiz Bot. Todos los derechos reservados.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CreatorCard(
    name: String,
    role: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = role,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF00CC44),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Justify
        )
    }
}
