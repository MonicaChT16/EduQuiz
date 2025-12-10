package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingRepository
import com.eduquiz.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RankingTab {
    SCHOOL,    // Mi Colegio/UGEL
    NATIONAL   // Nacional
}

data class UserRankingStats(
    val position: Int,
    val totalScore: Int,
    val accuracy: Float,
    val examsCompleted: Int
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val profileRepository: ProfileRepository,
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()

    private var currentJob: Job? = null

    fun start(uid: String) {
        _state.update { it.copy(currentUid = uid) }
        // Cargar perfil del usuario y calcular stats
        loadUserStats(uid)
        // Por defecto mostrar vista nacional
        loadNationalLeaderboard()
    }

    private fun loadUserStats(uid: String) {
        viewModelScope.launch {
            try {
                // Obtener perfil
                val profile = profileRepository.observeProfile(uid)
                    .firstOrNull()
                
                // Calcular stats desde la base de datos local
                val attempts = examRepository.getAttempts(uid)
                val completedAttempts = attempts.filter { 
                    it.status == ExamStatus.COMPLETED || it.status == ExamStatus.AUTO_SUBMIT 
                }
                val examsCompleted = completedAttempts.size
                
                var totalCorrect = 0
                var totalAnswered = 0
                
                completedAttempts.forEach { attempt ->
                    val answers = examRepository.getAnswersForAttempt(attempt.attemptId)
                    totalAnswered += answers.size
                    totalCorrect += answers.count { it.isCorrect }
                }
                
                val accuracy = if (totalAnswered > 0) {
                    (totalCorrect.toFloat() / totalAnswered.toFloat()) * 100f
                } else {
                    0f
                }
                
                val totalScore = profile?.xp?.toInt() ?: 0
                
                _state.update { 
                    it.copy(
                        userStats = UserRankingStats(
                            position = 0, // Se calculará cuando se cargue el ranking
                            totalScore = totalScore,
                            accuracy = accuracy,
                            examsCompleted = examsCompleted
                        ),
                        userDisplayName = profile?.displayName,
                        userPhotoUrl = profile?.photoUrl
                    ) 
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error loading user stats", e)
            }
        }
    }

    fun switchTab(tab: RankingTab) {
        if (_state.value.currentTab == tab) return
        
        currentJob?.cancel()
        _state.update { 
            it.copy(
                currentTab = tab,
                schoolCode = if (tab == RankingTab.SCHOOL) it.schoolCode else "",
                entries = emptyList(),
                isLoading = true,
                error = null
            ) 
        }
        
        when (tab) {
            RankingTab.SCHOOL -> {
                if (_state.value.schoolCode.isNotBlank()) {
                    loadSchoolLeaderboard(_state.value.schoolCode)
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
            RankingTab.NATIONAL -> loadNationalLeaderboard()
        }
    }

    fun searchSchool(schoolCode: String) {
        if (schoolCode.isBlank()) {
            _state.update { 
                it.copy(
                    schoolCode = "",
                    entries = emptyList(),
                    error = null
                ) 
            }
            return
        }

        currentJob?.cancel()
        _state.update { 
            it.copy(
                schoolCode = schoolCode,
                isLoading = true,
                error = null
            ) 
        }
        loadSchoolLeaderboard(schoolCode)
    }

    private fun loadSchoolLeaderboard(schoolCode: String) {
        currentJob = viewModelScope.launch {
            try {
                rankingRepository.observeSchoolLeaderboard(schoolCode)
                    .collect { entries ->
                        updateStateWithEntries(entries)
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el ranking del colegio"
                    )
                }
            }
        }
    }

    private fun loadNationalLeaderboard() {
        currentJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                rankingRepository.observeNationalLeaderboard()
                    .collect { entries ->
                        updateStateWithEntries(entries)
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el ranking nacional"
                    )
                }
            }
        }
    }

    private fun updateStateWithEntries(entries: List<LeaderboardEntry>) {
        val currentUid = _state.value.currentUid
        val sortedEntries = entries.sortedByDescending { it.totalScore }
        
        // Encontrar posición del usuario actual
        val userPosition = if (currentUid != null) {
            sortedEntries.indexOfFirst { it.uid == currentUid }.let { index ->
                if (index >= 0) index + 1 else 0
            }
        } else {
            0
        }
        
        var userEntry = sortedEntries.find { it.uid == currentUid }
        
        // Si el usuario no está en el ranking de Firestore, crear entrada desde stats locales
        if (userEntry == null && currentUid != null && _state.value.userStats != null) {
            val stats = _state.value.userStats!!
            userEntry = LeaderboardEntry(
                uid = currentUid,
                displayName = _state.value.userDisplayName ?: "Usuario",
                photoUrl = _state.value.userPhotoUrl,
                totalScore = stats.totalScore,
                accuracy = stats.accuracy,
                examsCompleted = stats.examsCompleted
            )
        }
        
        // Actualizar stats del usuario
        val currentUserStats = _state.value.userStats
        val updatedUserStats = if (userEntry != null && currentUid != null) {
            currentUserStats?.copy(
                position = if (userPosition > 0) userPosition else currentUserStats.position,
                totalScore = userEntry.totalScore,
                accuracy = userEntry.accuracy,
                examsCompleted = userEntry.examsCompleted
            ) ?: UserRankingStats(
                position = userPosition,
                totalScore = userEntry.totalScore,
                accuracy = userEntry.accuracy,
                examsCompleted = userEntry.examsCompleted
            )
        } else {
            currentUserStats?.copy(position = userPosition) ?: currentUserStats
        }
        
        // Determinar si el usuario está visible en la lista (top 100)
        val isUserVisible = userEntry != null && sortedEntries.take(100).any { it.uid == currentUid }
        
        _state.update {
            it.copy(
                isLoading = false,
                entries = sortedEntries.take(100), // Top 100
                error = null,
                userStats = updatedUserStats,
                isUserVisible = isUserVisible,
                userEntry = userEntry
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}

data class RankingState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val currentTab: RankingTab = RankingTab.NATIONAL,
    val schoolCode: String = "",
    val currentUid: String? = null,
    val userStats: UserRankingStats? = null,
    val isUserVisible: Boolean = false,
    val userEntry: LeaderboardEntry? = null,
    val userDisplayName: String? = null,
    val userPhotoUrl: String? = null
)