package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// BORRAMOS el import de domain que causaba error
// import com.eduquiz.domain.ranking.LeaderboardEntry 
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()

    fun start(uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, currentUid = uid) }
            
            // Simulamos red
            delay(1000)

            // Datos falsos (Dummy) para que funcione la UI
            val mockEntries = listOf(
                LeaderboardEntry("id_1", "Ana García", 1500),
                LeaderboardEntry(uid, "Tú", 1350),
                LeaderboardEntry("id_3", "Carlos Diaz", 1200),
                LeaderboardEntry("id_4", "Lucía Mendez", 900)
            )

            _state.update { 
                it.copy(
                    isLoading = false, 
                    entries = mockEntries,
                    classroomLabel = "Aula Demo"
                ) 
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

// --- AQUÍ DEFINIMOS LA CLASE PARA QUE NO DE ERROR ---
// Esto sustituye al archivo que te falta del dominio
data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val totalScore: Int
)