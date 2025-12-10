package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingRepository
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.sync.SyncRepository
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
    private val examRepository: ExamRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()

    private var currentJob: Job? = null

    fun start(uid: String) {
        _state.update { it.copy(currentUid = uid) }
        // Cargar perfil del usuario y calcular stats
        loadUserStats(uid)
        // Observar cambios en el código UGEL del usuario (persistente)
        observeUserUgelCode(uid)
        // Por defecto mostrar vista nacional
        loadNationalLeaderboard()
    }
    
    /**
     * Observa el código UGEL del usuario y lo mantiene actualizado en el estado.
     * Esto asegura que el código persista incluso cuando el usuario cambia de pestaña.
     */
    private fun observeUserUgelCode(uid: String) {
        viewModelScope.launch {
            try {
                profileRepository.observeProfile(uid).collect { profile ->
                    val ugelCode = profile?.ugelCode?.takeIf { it.isNotBlank() } ?: ""
                    val currentUserCode = _state.value.userUgelCode
                    
                    // Solo actualizar si el código cambió
                    if (ugelCode != currentUserCode) {
                        _state.update { 
                            it.copy(
                                userUgelCode = ugelCode, // Guardar código del usuario (persistente)
                                // Si estamos en pestaña SCHOOL y no hay código de búsqueda, usar el del usuario
                                schoolCode = if (it.currentTab == RankingTab.SCHOOL && it.schoolCode.isBlank()) {
                                    ugelCode
                                } else {
                                    it.schoolCode // Mantener el código de búsqueda si existe
                                },
                                // Si tiene código UGEL y estamos en NATIONAL, cambiar a SCHOOL
                                currentTab = if (ugelCode.isNotBlank() && it.currentTab == RankingTab.NATIONAL) {
                                    RankingTab.SCHOOL
                                } else {
                                    it.currentTab
                                }
                            ) 
                        }
                        
                        // Si tiene código UGEL y estamos en pestaña SCHOOL, cargar su ranking
                        if (ugelCode.isNotBlank() && _state.value.currentTab == RankingTab.SCHOOL) {
                            loadSchoolLeaderboard(ugelCode)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error observing user UGEL code", e)
            }
        }
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
        
        // Mantener el código UGEL del usuario siempre visible
        val userCode = _state.value.userUgelCode
        val codeToUse = if (tab == RankingTab.SCHOOL && userCode.isNotBlank()) {
            userCode // Usar el código del usuario si está en pestaña SCHOOL
        } else if (tab == RankingTab.SCHOOL) {
            _state.value.schoolCode // Mantener el código de búsqueda si existe
        } else {
            "" // En NATIONAL no se necesita código
        }
        
        _state.update { 
            it.copy(
                currentTab = tab,
                schoolCode = codeToUse, // Mantener el código visible
                entries = emptyList(),
                isLoading = true,
                error = null
            ) 
        }
        
        when (tab) {
            RankingTab.SCHOOL -> {
                // Si hay código (del usuario o de búsqueda), cargar ranking
                if (codeToUse.isNotBlank()) {
                    loadSchoolLeaderboard(codeToUse)
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
            RankingTab.NATIONAL -> loadNationalLeaderboard()
        }
    }

    fun searchSchool(schoolCode: String) {
        val trimmedCode = schoolCode.trim()
        
        if (trimmedCode.isBlank()) {
            _state.update { 
                it.copy(
                    schoolCode = "",
                    entries = emptyList(),
                    error = null
                ) 
            }
            return
        }

        // Validar que el código tenga exactamente 7 cifras
        if (trimmedCode.length != 7 || !trimmedCode.all { it.isDigit() }) {
            _state.update { 
                it.copy(
                    error = "El código UGEL debe tener exactamente 7 dígitos numéricos",
                    isLoading = false
                ) 
            }
            return
        }

        currentJob?.cancel()
        _state.update { 
            it.copy(
                schoolCode = trimmedCode,
                isLoading = true,
                error = null
            ) 
        }
        
        // Guardar el código UGEL en el perfil del usuario (solo si es diferente al actual)
        val currentUid = _state.value.currentUid
        if (currentUid != null) {
            viewModelScope.launch {
                try {
                    val currentProfile = profileRepository.observeProfile(currentUid).firstOrNull()
                    // Solo actualizar si el código es diferente al actual
                    if (currentProfile?.ugelCode != trimmedCode) {
                        val now = System.currentTimeMillis()
                        profileRepository.updateUgelCode(
                            uid = currentUid,
                            ugelCode = trimmedCode,
                            updatedAtLocal = now,
                            syncState = SyncState.PENDING
                        )
                        // Encolar sincronización inmediata para que se vea en Firestore
                        syncRepository.enqueueSyncNow()
                        android.util.Log.d("RankingViewModel", "UGEL code saved: $trimmedCode")
                        
                        // Actualizar el código del usuario en el estado (persistente)
                        _state.update { 
                            it.copy(
                                userUgelCode = trimmedCode, // Actualizar código del usuario
                                schoolCode = trimmedCode // Actualizar código de búsqueda
                            ) 
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RankingViewModel", "Error saving UGEL code", e)
                    _state.update { 
                        it.copy(
                            error = "Error al guardar el código UGEL: ${e.message}",
                            isLoading = false
                        ) 
                    }
                }
            }
        }
        
        loadSchoolLeaderboard(trimmedCode)
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
    val schoolCode: String = "", // Código actual para búsqueda/filtro
    val userUgelCode: String = "", // Código UGEL del usuario (siempre persistente)
    val currentUid: String? = null,
    val userStats: UserRankingStats? = null,
    val isUserVisible: Boolean = false,
    val userEntry: LeaderboardEntry? = null,
    val userDisplayName: String? = null,
    val userPhotoUrl: String? = null
)