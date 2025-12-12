package com.eduquiz.feature.ranking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.feature.ranking.RankingTab
import com.eduquiz.feature.ranking.SortBy

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RankingFeature(
    uid: String,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(uid) {
        viewModel.start(uid)
    }

    // Actualización automática cada 5 segundos (silenciosa)
    LaunchedEffect(state.currentTab, state.schoolCode) {
        while (true) {
            delay(5000) // 5 segundos
            if (!state.isLoading) {
                viewModel.refreshSilent()
            }
        }
    }
    val listState = rememberLazyListState()

    Surface(modifier = modifier.fillMaxSize()) {
        Box {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Encabezado con perfil del usuario
                UserHeader(
                    uid = uid,
                    userStats = state.userStats,
                    displayName = state.userDisplayName,
                    photoUrl = state.userPhotoUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pestañas (ahora 3: Aula, Colegio, Nacional)
                    TabSelector(
                        selectedTab = state.currentTab,
                        onTabSelected = { tab ->
                            viewModel.switchTab(tab)
                        }
                    )

                // Selector de ordenamiento
                SortSelector(
                    sortBy = state.sortBy,
                    onSortSelected = { sort ->
                        viewModel.setSortBy(sort)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                    // Campo de búsqueda (solo para pestaña Colegio)
                    if (state.currentTab == RankingTab.SCHOOL) {
                        SchoolCodeSearch(
                            schoolCode = state.userUgelCode.ifBlank { state.schoolCode },
                            onSearch = { code ->
                                viewModel.searchSchool(code)
                            },
                            isUserCode = state.userUgelCode.isNotBlank()
                        )
                    }

                    // Contenido
                    when {
                        state.error != null -> ErrorState(message = state.error!!)
                        state.isLoading -> LoadingState()
                        else -> {
                            Box(modifier = Modifier.weight(1f)) {
                                RankingList(
                                    entries = state.entries,
                                    currentUid = state.currentUid,
                                    listState = listState,
                                    isLoadingMore = state.isLoadingMore,
                                    onLoadMore = { viewModel.loadMore() },
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Sticky row si el usuario no está visible
                                val userEntry = state.userEntry
                                val userStats = state.userStats
                                if (!state.isUserVisible && userEntry != null && userStats != null) {
                                    StickyUserRow(
                                        userEntry = userEntry,
                                        userStats = userStats,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabSelector(
    selectedTab: RankingTab,
    onTabSelected: (RankingTab) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedTab == RankingTab.SCHOOL,
            onClick = { onTabSelected(RankingTab.SCHOOL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("Mi Colegio")
        }
        SegmentedButton(
            selected = selectedTab == RankingTab.NATIONAL,
            onClick = { onTabSelected(RankingTab.NATIONAL) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("Nacional")
        }
    }
}

@Composable
private fun SortSelector(
    sortBy: SortBy,
    onSortSelected: (SortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ordenar por:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Box {
            Button(
                onClick = { expanded = true }
            ) {
                Text(
                    text = when (sortBy) {
                        SortBy.SCORE -> "XP"
                        SortBy.ACCURACY -> "Precisión"
                        SortBy.EXAMS -> "Exámenes"
                    }
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("XP") },
                    onClick = {
                        onSortSelected(SortBy.SCORE)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Precisión") },
                    onClick = {
                        onSortSelected(SortBy.ACCURACY)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exámenes") },
                    onClick = {
                        onSortSelected(SortBy.EXAMS)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SchoolCodeSearch(
    schoolCode: String,
    onSearch: (String) -> Unit,
    isUserCode: Boolean = false
) {
    var inputText by remember(schoolCode) { mutableStateOf(schoolCode) }
    
    LaunchedEffect(schoolCode) {
        if (inputText != schoolCode) {
            inputText = schoolCode
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isUserCode && schoolCode.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Tu código UGEL: $schoolCode - Solo verás estudiantes con este código",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 7) {
                        inputText = newValue
                    }
                },
                label = { Text("Código de colegio (UGEL)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Ej: 1234567") },
                enabled = !isUserCode,
                supportingText = {
                    when {
                        isUserCode && schoolCode.isNotBlank() -> Text(
                            "Puedes cambiar tu código si ingresas uno nuevo",
                            style = MaterialTheme.typography.bodySmall
                        )
                        schoolCode.isNotBlank() -> Text(
                            "Código actual: $schoolCode",
                            style = MaterialTheme.typography.bodySmall
                        )
                        inputText.length < 7 && inputText.isNotEmpty() -> Text(
                            "Faltan ${7 - inputText.length} dígitos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> Text(
                            "Ingresa 7 dígitos numéricos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                isError = inputText.isNotEmpty() && (inputText.length != 7 || !inputText.all { it.isDigit() })
            )
            TextButton(
                onClick = { onSearch(inputText.trim()) },
                enabled = inputText.trim().length == 7 && inputText.trim().all { it.isDigit() }
            ) {
                Text(if (isUserCode && inputText.trim() == schoolCode) "Cambiar" else "Unirse/Ver")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Cargando ranking…")
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No pudimos cargar el ranking", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RankingList(
    entries: List<LeaderboardEntry>,
    currentUid: String?,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No hay datos disponibles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.uid }) { index, entry ->
                RankingRow(
                    position = index + 1,
                    entry = entry,
                    isCurrentUser = entry.uid == currentUid
                )
            }
            
            // Botón de cargar más
            if (entries.size >= 100) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator()
                        } else {
                            Button(onClick = onLoadMore) {
                                Text("Cargar más")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    position: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#$position",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = entry.displayName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.displayName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${entry.totalScore} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${String.format("%.1f", entry.accuracy)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${entry.examsCompleted} exámenes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserHeader(
    uid: String,
    userStats: UserRankingStats?,
    displayName: String?,
    photoUrl: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Tu perfil",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName ?: "Usuario",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (userStats != null && userStats.position > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Puesto #${userStats.position}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        // Indicador de cambio de posición
                        userStats.previousPosition?.let { previous ->
                            if (previous > 0 && previous != userStats.position) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Icon(
                                        imageVector = if (userStats.position < previous) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = if (userStats.position < previous) "Subió" else "Bajó",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (userStats.position < previous) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (userStats != null) {
                    Text(
                        text = "No clasificado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (userStats != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${userStats.totalScore} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.1f", userStats.accuracy)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${userStats.examsCompleted} exámenes",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyUserRow(
    userEntry: LeaderboardEntry,
    userStats: UserRankingStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "#${userStats.position}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    // Indicador de cambio
                    userStats.previousPosition?.let { previous ->
                        if (previous > 0 && previous != userStats.position) {
                            Icon(
                                imageVector = if (userStats.position < previous) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (userStats.position < previous) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
                
                AsyncImage(
                    model = userEntry.photoUrl,
                    contentDescription = "Tu perfil",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Text(
                    text = userEntry.displayName.ifBlank { "Tú" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${userStats.totalScore} XP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", userStats.accuracy)}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${userStats.examsCompleted} exámenes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
