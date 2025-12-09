package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()

    fun start(uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, currentUid = uid) }

            try {
                // --- SOLUCIÓN DEL ERROR ---
                // 1. Usamos el nombre correcto: observeClassroomLeaderboard
                // 2. Pasamos IDs de prueba ("default") porque la función los exige.
                // 3. Usamos .collect { ... } porque devuelve un Flow (flujo de datos).
                rankingRepository.observeClassroomLeaderboard("school_default", "class_default")
                    .collect { listaRanking ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                entries = listaRanking,
                                classroomLabel = "Aula: Default"
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }
}

data class RankingState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val classroomLabel: String? = null,
    val currentUid: String? = null
)