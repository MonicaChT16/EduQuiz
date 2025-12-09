package com.eduquiz.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eduquiz.domain.store.Cosmetic
import com.eduquiz.feature.auth.presentation.AuthViewModel

@Composable
fun StoreFeature(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    storeViewModel: StoreViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val state by storeViewModel.state.collectAsStateWithLifecycle()

    val user = (authState as? com.eduquiz.feature.auth.model.AuthState.Authenticated)?.user

    LaunchedEffect(user?.uid) {
        user?.uid?.let { storeViewModel.initialize(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con coins
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.currentCoins}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.isLoading && state.catalog.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else {
            Text(
                text = "Catálogo de Cosméticos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.catalog) { cosmetic ->
                    CosmeticCard(
                        cosmetic = cosmetic,
                        isPurchased = state.purchasedCosmetics.contains(cosmetic.cosmeticId),
                        isEquipped = state.selectedCosmeticId == cosmetic.cosmeticId,
                        canAfford = state.currentCoins >= cosmetic.cost,
                        onPurchase = { storeViewModel.purchaseCosmetic(cosmetic.cosmeticId) },
                        onEquip = { storeViewModel.equipCosmetic(cosmetic.cosmeticId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CosmeticCard(
    cosmetic: Cosmetic,
    isPurchased: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview del cosmético sobre una foto de perfil de ejemplo
            CosmeticPreview(
                overlayUrl = cosmetic.overlayImageUrl,
                cosmeticName = cosmetic.name,
                cosmeticId = cosmetic.cosmeticId,
                modifier = Modifier.size(80.dp)
            )
            
            Text(
                text = cosmetic.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            cosmetic.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${cosmetic.cost} EduCoins",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (canAfford || isPurchased) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            if (isEquipped) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text(text = "Equipado")
                }
            } else if (isPurchased) {
                Button(
                    onClick = onEquip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Equipar")
                }
            } else {
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canAfford
                ) {
                    Text(text = if (canAfford) "Comprar" else "Insuficientes coins")
                }
            }
        }
    }
}

/**
 * Preview del cosmético mostrando cómo se verá sobre una foto de perfil.
 */
@Composable
private fun CosmeticPreview(
    overlayUrl: String?,
    cosmeticName: String,
    cosmeticId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Foto de perfil de ejemplo (placeholder con gradiente)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder con iniciales o icono
            Text(
                text = cosmeticName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Marco básico: borde simple para el primer cosmético
        if (cosmeticId == "basic_frame") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
        
        // Overlay del cosmético (solo si la URL es válida y no es de ejemplo)
        overlayUrl?.let { url ->
            if (!url.contains("example.com") && url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = "Preview del cosmético $cosmeticName",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onError = {
                        // Si falla la carga, no mostrar nada (ya tenemos el placeholder)
                    }
                )
            }
        }
    }
}
