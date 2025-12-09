package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RankingUiState(
    val isLoading: Boolean = true,
    val entries: List<LeaderboardEntry> = emptyList(),
    val error: String? = null,
    val classroomLabel: String? = null,
    val currentUid: String? = null,
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val uidState = MutableStateFlow<String?>(null)

    val state: StateFlow<RankingUiState> = uidState
        .filterNotNull()
        .flatMapLatest { uid ->
            profileRepository.observeProfile(uid)
                .filterNotNull()
                .flatMapLatest { profile ->
                    val schoolId = profile.schoolId
                    val classroomId = profile.classroomId
                    if (schoolId.isBlank() || classroomId.isBlank()) {
                        // Emitimos error si falta la relación al aula.
                        flowOf(
                            RankingUiState(
                                isLoading = false,
                                error = "Tu perfil no tiene aula asignada",
                                classroomLabel = null,
                                currentUid = uid
                            )
                        )
                    } else {
                        rankingRepository
                            .observeClassroomLeaderboard(schoolId, classroomId)
                            .map { entries ->
                                RankingUiState(
                                    isLoading = false,
                                    entries = entries,
                                    error = null,
                                    classroomLabel = "$schoolId / $classroomId",
                                    currentUid = uid
                                )
                            }
                    }
                }
        }
        .catch { emit(RankingUiState(isLoading = false, error = it.message ?: "Error desconocido")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RankingUiState()
        )

    fun start(uid: String) {
        viewModelScope.launch {
            uidState.emit(uid)
        }
    }
}

