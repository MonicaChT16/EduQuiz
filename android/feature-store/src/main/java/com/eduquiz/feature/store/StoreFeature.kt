package com.eduquiz.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tienda",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                CoinBalanceChip(coins = state.currentCoins)
            }

            CategoryChipsRow()
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (state.isLoading && state.catalog.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
    // Mantener el primer ítem actual ("Marco Básico")
    if (cosmetic.cosmeticId == "basic_frame") {
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

                StoreActionButton(
                    isEquipped = isEquipped,
                    isPurchased = isPurchased,
                    canAfford = canAfford,
                    onPurchase = onPurchase,
                    onEquip = onEquip
                )
            }
        }
        return
    }

    val showDiscountBadge = cosmetic.name.contains("bundle", ignoreCase = true) ||
        cosmetic.name.contains("lote", ignoreCase = true)
    val discountedCost = (cosmetic.cost * 0.51f).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(Color(0xFFE6E8EE), Color(0xFFF3F4F7))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFEAF2FF), Color(0xFFF7FAFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CosmeticPreview(
                    overlayUrl = cosmetic.overlayImageUrl,
                    cosmeticName = cosmetic.name,
                    cosmeticId = cosmetic.cosmeticId,
                    modifier = Modifier.size(88.dp)
                )

                if (showDiscountBadge) {
                    DiscountBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        text = "-49%"
                    )
                }
            }

            Text(
                text = cosmetic.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            cosmetic.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinDot()
                    Spacer(Modifier.width(6.dp))
                    if (showDiscountBadge) {
                        Text(
                            text = "${cosmetic.cost}",
                            style = MaterialTheme.typography.bodySmall.merge(
                                TextStyle(textDecoration = TextDecoration.LineThrough)
                            ),
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$discountedCost",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${cosmetic.cost}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                StoreActionButton(
                    isEquipped = isEquipped,
                    isPurchased = isPurchased,
                    canAfford = canAfford,
                    onPurchase = onPurchase,
                    onEquip = onEquip,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun StoreActionButton(
    isEquipped: Boolean,
    isPurchased: Boolean,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    val modifier = if (compact) {
        Modifier.wrapContentHeight()
    } else {
        Modifier.fillMaxWidth()
    }

    when {
        isEquipped -> {
            OutlinedButton(onClick = {}, enabled = false, shape = shape, modifier = modifier) {
                Text("Equipado")
            }
        }
        isPurchased -> {
            Button(onClick = onEquip, shape = shape, modifier = modifier) {
                Text("Equipar")
            }
        }
        else -> {
            Button(
                onClick = onPurchase,
                enabled = canAfford,
                shape = shape,
                modifier = modifier
            ) {
                Text(if (canAfford) "Comprar" else "Insuficientes")
            }
        }
    }
}

@Composable
private fun CoinBalanceChip(coins: Int) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinDot()
            Spacer(Modifier.width(6.dp))
            Text(
                text = coins.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryChipsRow() {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text("Todo") },
            leadingIcon = { ChipSymbol("▦") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color.White,
                selectedLabelColor = Color(0xFF1F2937),
                containerColor = Color.Transparent,
                labelColor = Color.White
            )
        )

        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("Iconos") },
            leadingIcon = { ChipSymbol("★") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = Color.White.copy(alpha = 0.8f)
            )
        )

        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("Efectos") },
            leadingIcon = { ChipSymbol("✦") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = Color.White.copy(alpha = 0.8f)
            )
        )

        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("Premium") },
            leadingIcon = { ChipSymbol("♛") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = Color.White.copy(alpha = 0.8f)
            )
        )
    }
}

@Composable
private fun DiscountBadge(modifier: Modifier = Modifier, text: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE83F3F))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CoinDot() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFB02E))
    )
}

@Composable
private fun ChipSymbol(symbol: String) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
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
        modifier = modifier.clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
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
            Text(
                text = cosmeticName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

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

        overlayUrl?.let { url ->
            if (!url.contains("example.com") && url.isNotBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = "Preview del cosmético $cosmeticName",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onError = { }
                )
            }
        }
    }
}
