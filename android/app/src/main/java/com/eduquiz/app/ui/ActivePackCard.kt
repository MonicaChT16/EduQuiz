package com.eduquiz.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ActivePackViewModel @Inject constructor(
    private val packRepository: PackRepository,
) : ViewModel() {

    val activePack: StateFlow<Pack?> = packRepository.observeActivePack()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

}

@Composable
fun ActivePackCard(
    modifier: Modifier = Modifier,
    viewModel: ActivePackViewModel = hiltViewModel()
) {
    val pack by viewModel.activePack.collectAsStateWithLifecycle()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pack activo",
                style = MaterialTheme.typography.titleMedium
            )
            if (pack == null) {
                Text(
                    text = "Sin pack activo. Descarga uno para empezar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = pack!!.weekLabel,
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Estado: ${pack!!.status}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ID: ${pack!!.packId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
