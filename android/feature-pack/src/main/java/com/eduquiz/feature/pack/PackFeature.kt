package com.eduquiz.feature.pack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackMeta
import com.eduquiz.domain.pack.PackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PackScreenStatus {
    NoPack, Disponible, Descargado, Actualizando
}

data class PackUiState(
    val activePack: Pack? = null,
    val availablePack: PackMeta? = null,
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null,
    val status: PackScreenStatus = PackScreenStatus.NoPack,
) {
    fun withStatus(
        active: Pack? = activePack,
        available: PackMeta? = availablePack,
        loading: Boolean = isLoading,
        downloading: Boolean = isDownloading,
    ): PackUiState {
        val derivedStatus = when {
            downloading || loading -> PackScreenStatus.Actualizando
            active != null && available != null && active.packId != available.packId -> PackScreenStatus.Disponible
            active != null -> PackScreenStatus.Descargado
            available != null -> PackScreenStatus.Disponible
            else -> PackScreenStatus.NoPack
        }
        return copy(status = derivedStatus)
    }
}

@HiltViewModel
class PackViewModel @Inject constructor(
    private val packRepository: PackRepository,
) : ViewModel() {

    private val activePack: StateFlow<Pack?> = packRepository.observeActivePack()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    private val _state = MutableStateFlow(PackUiState())
    val state: StateFlow<PackUiState> = _state.asStateFlow()

    init {
        observeActivePack()
        refreshCurrentPack()
    }

    private fun observeActivePack() {
        viewModelScope.launch {
            activePack.collect { pack ->
                _state.update { current ->
                    current.copy(activePack = pack).withStatus(active = pack)
                }
            }
        }
    }

    fun refreshCurrentPack() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null).withStatus(loading = true) }
            val meta = runCatching { packRepository.fetchCurrentPackMeta() }.getOrElse { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Error al consultar el pack actual."
                    ).withStatus(active = activePack.value)
                }
                null
            }
            _state.update { current ->
                current.copy(
                    availablePack = meta,
                    isLoading = false
                ).withStatus(active = activePack.value, available = meta)
            }
        }
    }

    fun downloadCurrentPack() {
        val packId = _state.value.availablePack?.packId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDownloading = true, errorMessage = null).withStatus(downloading = true) }
            runCatching { packRepository.downloadPack(packId) }
                .onSuccess { downloaded ->
                    _state.update { current ->
                        current.copy(
                            isDownloading = false,
                            activePack = downloaded,
                            errorMessage = null
                        ).withStatus(active = downloaded)
                    }
                }
                .onFailure { throwable ->
                    _state.update { current ->
                        current.copy(
                            isDownloading = false,
                            errorMessage = throwable.localizedMessage
                                ?: "No se pudo descargar el pack."
                        ).withStatus(active = activePack.value)
                    }
                }
        }
    }
}

@Composable
fun PackFeature(
    onStartExam: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PackViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PackScreen(
        state = state,
        onDownloadClick = viewModel::downloadCurrentPack,
        onRetryClick = viewModel::refreshCurrentPack,
        onStartExam = onStartExam,
        modifier = modifier
    )
}

@Composable
fun PackScreen(
    state: PackUiState,
    onDownloadClick: () -> Unit,
    onRetryClick: () -> Unit,
    onStartExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = state.isDownloading || state.isLoading
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Pack semanal", style = MaterialTheme.typography.headlineMedium)
            StatusPill(status = state.status)
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PackDetailsCard(
                title = "Pack disponible",
                weekLabel = state.availablePack?.weekLabel,
                packId = state.availablePack?.packId,
                emptyMessage = "Aun no hay pack publicado. Intenta refrescar.",
                badge = if (state.availablePack != null &&
                    state.activePack?.packId == state.availablePack.packId
                ) "Descargado" else null
            )
            PackDetailsCard(
                title = "Pack activo offline",
                weekLabel = state.activePack?.weekLabel,
                packId = state.activePack?.packId,
                emptyMessage = "No tienes packs guardados en este dispositivo."
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDownloadClick,
                    enabled = state.availablePack != null &&
                        !busy &&
                        state.availablePack.packId != state.activePack?.packId,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = "Descargar Pack de la Semana")
                }
                TextButton(
                    onClick = onRetryClick,
                    enabled = !busy,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(text = "Refrescar")
                }
            }
            Button(
                onClick = onStartExam,
                enabled = state.activePack != null && !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Iniciar simulacro con pack activo")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusPill(status: PackScreenStatus) {
    val (label, container, content) = when (status) {
        PackScreenStatus.NoPack -> Triple("Sin pack", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
        PackScreenStatus.Disponible -> Triple("Pack disponible", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        PackScreenStatus.Descargado -> Triple("Descargado", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        PackScreenStatus.Actualizando -> Triple("Actualizando", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = content,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun PackDetailsCard(
    title: String,
    weekLabel: String?,
    packId: String?,
    emptyMessage: String,
    badge: String? = null,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (badge != null) {
                    Text(
                        text = badge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (weekLabel == null) {
                Text(text = emptyMessage, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = weekLabel, style = MaterialTheme.typography.titleLarge)
                if (packId != null) {
                    Text(
                        text = "ID: $packId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PackScreenPreview() {
    val state = PackUiState(
        activePack = Pack(
            packId = "pack-1",
            weekLabel = "Semana 1",
            status = "ACTIVE",
            publishedAt = 1L,
            downloadedAt = 1L
        ),
        availablePack = PackMeta(
            packId = "pack-2",
            weekLabel = "Semana 2",
            status = "PUBLISHED",
            publishedAt = 2L,
            textIds = emptyList(),
            questionIds = emptyList()
        ),
        status = PackScreenStatus.Disponible
    )
    PackScreen(
        state = state,
        onDownloadClick = {},
        onRetryClick = {},
        onStartExam = {}
    )
}
