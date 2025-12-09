package com.eduquiz.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
