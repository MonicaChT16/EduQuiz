package com.eduquiz.feature.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.exam.ExamStatus
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.ranking.LeaderboardEntry
import com.eduquiz.domain.ranking.RankingError
import com.eduquiz.domain.ranking.RankingRepository
import com.eduquiz.domain.ranking.RankingResult
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.profile.UserProfile
import com.eduquiz.domain.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RankingTab {
    SCHOOL,     // Mi Colegio/UGEL
    NATIONAL    // Nacional
}

enum class SortBy {
    SCORE,      // Por XP/Score
    ACCURACY,   // Por precisión
    EXAMS       // Por exámenes completados
}

data class UserRankingStats(
    val position: Int,
    val totalScore: Int,
    val accuracy: Float,
    val examsCompleted: Int,
    val previousPosition: Int? = null // Para mostrar cambios
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
    private var hasMorePages = true

    fun start(uid: String) {
        _state.update { it.copy(currentUid = uid) }
        loadUserStats(uid)
        observeUserUgelCode(uid)
        // Por defecto mostrar vista nacional
        loadNationalLeaderboard()
    }
    
    private fun observeUserUgelCode(uid: String) {
        viewModelScope.launch {
            try {
                profileRepository.observeProfile(uid).collect { profile ->
                    val ugelCode = profile?.ugelCode?.takeIf { it.isNotBlank() } ?: ""
                    val currentUserCode = _state.value.userUgelCode
                    
                    if (ugelCode != currentUserCode) {
                        _state.update { 
                            it.copy(
                                userUgelCode = ugelCode,
                                schoolCode = if (it.currentTab == RankingTab.SCHOOL && it.schoolCode.isBlank()) {
                                    ugelCode
                                } else {
                                    it.schoolCode
                                }
                            ) 
                        }
                        
                        // Si tiene datos y estamos en la pestaña correspondiente, recargar
                        when (_state.value.currentTab) {
                            RankingTab.SCHOOL -> {
                                if (ugelCode.isNotBlank()) {
                                    loadSchoolLeaderboard(ugelCode)
                                }
                            }
                            RankingTab.NATIONAL -> { /* No hacer nada */ }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error observing user profile", e)
            }
        }
    }

    private fun loadUserStats(uid: String) {
        // Observar cambios en el perfil y actualizar estadísticas continuamente
        viewModelScope.launch {
            try {
                profileRepository.observeProfile(uid).collect { profile ->
                    updateUserStatsFromProfile(uid, profile)
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error observing user profile for stats", e)
            }
        }
        
        // Observar cambios en los intentos y actualizar estadísticas
        viewModelScope.launch {
            try {
                examRepository.observeAttempts(uid).collect { attempts ->
                    updateUserStatsFromAttempts(uid, attempts)
                }
            } catch (e: Exception) {
                android.util.Log.e("RankingViewModel", "Error observing attempts for stats", e)
            }
        }
    }
    
    private suspend fun updateUserStatsFromProfile(uid: String, profile: UserProfile?) {
        val currentStats = _state.value.userStats
        val totalScore = profile?.xp?.toInt() ?: 0
        
        // Solo actualizar si el XP cambió
        if (currentStats?.totalScore != totalScore) {
            updateUserStats(
                totalScore = totalScore,
                displayName = profile?.displayName,
                photoUrl = profile?.photoUrl,
                selectedCosmeticId = profile?.selectedCosmeticId
            )
        } else {
            // Actualizar solo displayName, photoUrl y selectedCosmeticId si el XP es el mismo
            _state.update {
                it.copy(
                    userDisplayName = profile?.displayName ?: it.userDisplayName,
                    userPhotoUrl = profile?.photoUrl ?: it.userPhotoUrl,
                    userSelectedCosmeticId = profile?.selectedCosmeticId
                )
            }
        }
    }
    
    private suspend fun updateUserStatsFromAttempts(uid: String, attempts: List<ExamAttempt>) {
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
        
        val currentStats = _state.value.userStats
        val totalScore = currentStats?.totalScore ?: (profileRepository.observeProfile(uid).firstOrNull()?.xp?.toInt() ?: 0)
        
        updateUserStats(
            totalScore = totalScore,
            accuracy = accuracy,
            examsCompleted = examsCompleted
        )
    }
    
    private fun updateUserStats(
        totalScore: Int? = null,
        accuracy: Float? = null,
        examsCompleted: Int? = null,
        displayName: String? = null,
        photoUrl: String? = null,
        selectedCosmeticId: String? = null
    ) {
        val currentStats = _state.value.userStats
        val previousPosition = currentStats?.position
        
        val updatedStats = UserRankingStats(
            position = currentStats?.position ?: 0,
            totalScore = totalScore ?: currentStats?.totalScore ?: 0,
            accuracy = accuracy ?: currentStats?.accuracy ?: 0f,
            examsCompleted = examsCompleted ?: currentStats?.examsCompleted ?: 0,
            previousPosition = previousPosition
        )
        
        // Verificar si hay cambios que necesitan sincronización
        val needsSync = currentStats == null || 
            updatedStats.totalScore != currentStats.totalScore ||
            updatedStats.accuracy != currentStats.accuracy ||
            updatedStats.examsCompleted != currentStats.examsCompleted
        
        android.util.Log.d("RankingViewModel", "📊 Checking if sync needed - needsSync: $needsSync, currentUid: ${_state.value.currentUid}")
        if (currentStats != null) {
            android.util.Log.d("RankingViewModel", "Current stats - totalScore: ${currentStats.totalScore}, accuracy: ${currentStats.accuracy}, exams: ${currentStats.examsCompleted}")
        }
        android.util.Log.d("RankingViewModel", "Updated stats - totalScore: ${updatedStats.totalScore}, accuracy: ${updatedStats.accuracy}, exams: ${updatedStats.examsCompleted}")
        
        // Sincronizar inmediatamente si hay cambios
        if (needsSync && _state.value.currentUid != null) {
            val uid = _state.value.currentUid!!
            android.util.Log.d("RankingViewModel", "🔄 Changes detected! Syncing profile for $uid")
            viewModelScope.launch {
                try {
                    // Sincronizar inmediatamente el perfil
                    val success = syncRepository.syncUserProfileNow(uid)
                    android.util.Log.d("RankingViewModel", "✅ Sync completed for $uid - result: $success")
                    // También encolar sync por si acaso
                    syncRepository.enqueueSyncNow()
                } catch (e: Exception) {
                    android.util.Log.e("RankingViewModel", "❌ Error during sync for $uid", e)
                }
            }
        } else {
            if (!needsSync) {
                android.util.Log.d("RankingViewModel", "⏭️ No sync needed - stats haven't changed")
            } else {
                android.util.Log.w("RankingViewModel", "⚠️ Sync needed but currentUid is null")
            }
        }
        
        _state.update { 
            it.copy(
                userStats = updatedStats,
                userDisplayName = displayName ?: it.userDisplayName,
                userPhotoUrl = photoUrl ?: it.userPhotoUrl,
                userSelectedCosmeticId = selectedCosmeticId ?: it.userSelectedCosmeticId
            ) 
        }
    }

    fun switchTab(tab: RankingTab) {
        if (_state.value.currentTab == tab) return
        
        currentJob?.cancel()
        resetPagination()
        
        val userCode = _state.value.userUgelCode
        val codeToUse = when {
            tab == RankingTab.SCHOOL && userCode.isNotBlank() -> userCode
            tab == RankingTab.SCHOOL -> _state.value.schoolCode
            else -> ""
        }
        
        _state.update { 
            it.copy(
                currentTab = tab,
                schoolCode = codeToUse,
                entries = emptyList(),
                isLoading = true,
                error = null,
                sortBy = SortBy.SCORE // Reset sort
            ) 
        }
        
        when (tab) {
            RankingTab.SCHOOL -> {
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

        // Validación mejorada
        when {
            trimmedCode.length != 7 -> {
                _state.update { 
                    it.copy(
                        error = "El código UGEL debe tener exactamente 7 dígitos",
                        isLoading = false
                    ) 
                }
                return
            }
            !trimmedCode.all { it.isDigit() } -> {
                _state.update { 
                    it.copy(
                        error = "El código UGEL solo puede contener números",
                        isLoading = false
                    ) 
                }
                return
            }
            trimmedCode.all { it == '0' } -> {
                _state.update { 
                    it.copy(
                        error = "El código UGEL no puede ser todos ceros",
                        isLoading = false
                    ) 
                }
                return
            }
        }

        currentJob?.cancel()
        resetPagination()
        _state.update { 
            it.copy(
                schoolCode = trimmedCode,
                isLoading = true,
                error = null
            ) 
        }
        
        val currentUid = _state.value.currentUid
        if (currentUid != null) {
            viewModelScope.launch {
                try {
                    val currentProfile = profileRepository.observeProfile(currentUid).firstOrNull()
                    if (currentProfile?.ugelCode != trimmedCode) {
                        val now = System.currentTimeMillis()
                        profileRepository.updateUgelCode(
                            uid = currentUid,
                            ugelCode = trimmedCode,
                            updatedAtLocal = now,
                            syncState = SyncState.PENDING
                        )
                        syncRepository.enqueueSyncNow()
                        android.util.Log.d("RankingViewModel", "UGEL code saved: $trimmedCode")
                        
                        _state.update { 
                            it.copy(
                                userUgelCode = trimmedCode,
                                schoolCode = trimmedCode
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

    fun refresh() {
        // Refresh manual con indicador
        _state.update { it.copy(isRefreshing = true) }
        resetPagination()
        
        viewModelScope.launch {
            try {
                when (_state.value.currentTab) {
                    RankingTab.SCHOOL -> {
                        val code = _state.value.schoolCode
                        if (code.isNotBlank()) {
                            loadSchoolLeaderboard(code)
                        }
                    }
                    RankingTab.NATIONAL -> loadNationalLeaderboard()
                }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }
    
    fun refreshSilent() {
        // Refresh silencioso sin indicador
        resetPagination()
        
        viewModelScope.launch {
            try {
                when (_state.value.currentTab) {
                    RankingTab.SCHOOL -> {
                        val code = _state.value.schoolCode
                        if (code.isNotBlank()) {
                            loadSchoolLeaderboard(code)
                        }
                    }
                    RankingTab.NATIONAL -> loadNationalLeaderboard()
                }
            } catch (e: Exception) {
                // Silencioso - solo log
                android.util.Log.d("RankingViewModel", "Silent refresh error: ${e.message}")
            }
        }
    }

    fun loadMore() {
        if (!hasMorePages || _state.value.isLoadingMore) return
        
        val currentEntries = _state.value.entries
        if (currentEntries.isEmpty()) return
        
        _state.update { it.copy(isLoadingMore = true) }
        
        viewModelScope.launch {
            try {
                val lastScore = currentEntries.lastOrNull()?.totalScore ?: 0
                val result = when (_state.value.currentTab) {
                    RankingTab.SCHOOL -> {
                        rankingRepository.loadMoreSchoolLeaderboard(
                            _state.value.schoolCode,
                            lastScore
                        )
                    }
                    RankingTab.NATIONAL -> {
                        rankingRepository.loadMoreNationalLeaderboard(lastScore)
                    }
                }
                
                when (result) {
                    is RankingResult.Success -> {
                        if (result.data.isEmpty()) {
                            hasMorePages = false
                            _state.update { it.copy(isLoadingMore = false) }
                        } else {
                            _state.update { 
                                it.copy(
                                    entries = it.entries + result.data,
                                    isLoadingMore = false
                                ) 
                            }
                            // Si hay menos de 100 resultados, no hay más páginas
                            if (result.data.size < 100) {
                                hasMorePages = false
                            }
                        }
                    }
                    is RankingResult.Error -> {
                        _state.update { 
                            it.copy(
                                isLoadingMore = false,
                                error = result.error.message
                            ) 
                        }
                        // Si es error de índice, deshabilitar paginación
                        if (result.error is RankingError.IndexMissing) {
                            hasMorePages = false
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Error al cargar más resultados"
                    ) 
                }
            }
        }
    }

    fun setSortBy(sortBy: SortBy) {
        if (_state.value.sortBy == sortBy) return
        
        _state.update { it.copy(sortBy = sortBy) }
        sortEntries()
    }

    private fun sortEntries() {
        val entries = _state.value.entries
        val sorted = sortEntriesWithTies(entries)
        
        _state.update { it.copy(entries = sorted) }
        updateUserPosition(sorted)
    }

    private fun loadSchoolLeaderboard(schoolCode: String) {
        currentJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                rankingRepository.observeSchoolLeaderboard(schoolCode)
                    .collect { result ->
                        handleRankingResult(result)
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
                    .collect { result ->
                        handleRankingResult(result)
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

    private fun handleRankingResult(result: RankingResult<List<LeaderboardEntry>>) {
        when (result) {
            is RankingResult.Success -> {
                updateStateWithEntries(result.data)
            }
            is RankingResult.Error -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.message,
                        entries = emptyList()
                    )
                }
            }
        }
    }

    private fun updateStateWithEntries(entries: List<LeaderboardEntry>) {
        val sortedEntries = sortEntriesWithTies(entries)
        updateUserPosition(sortedEntries)
        
        val currentUid = _state.value.currentUid
        var userEntry = sortedEntries.find { it.uid == currentUid }
        
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
        
        val currentUserStats = _state.value.userStats
        val userPosition = if (userEntry != null && currentUid != null) {
            sortedEntries.indexOfFirst { it.uid == currentUid }.let { index ->
                if (index >= 0) index + 1 else 0
            }
        } else {
            0
        }
        
        // Si el usuario no está en el top visible, calcular posición real
        if (userPosition == 0 && currentUid != null) {
            viewModelScope.launch {
                val positionResult = rankingRepository.calculateUserPosition(
                    currentUid,
                    if (_state.value.currentTab == RankingTab.SCHOOL) _state.value.schoolCode else null
                )
                when (positionResult) {
                    is RankingResult.Success -> {
                        _state.update {
                            it.copy(
                                userStats = it.userStats?.copy(position = positionResult.data)
                            )
                        }
                    }
                    is RankingResult.Error -> {
                        // Ignorar error de cálculo de posición
                    }
                }
            }
        }
        
        val updatedUserStats = if (userEntry != null && currentUid != null) {
            val previousPosition = currentUserStats?.position
            currentUserStats?.copy(
                position = if (userPosition > 0) userPosition else currentUserStats.position,
                previousPosition = previousPosition,
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
            currentUserStats
        }
        
        val isUserVisible = userEntry != null && sortedEntries.take(500).any { it.uid == currentUid }
        
        _state.update {
            it.copy(
                isLoading = false,
                isRefreshing = false, // Asegurar que isRefreshing se resetee
                entries = sortedEntries,
                error = null,
                userStats = updatedUserStats,
                isUserVisible = isUserVisible,
                userEntry = userEntry
            )
        }
    }

    /**
     * Ordena entradas con manejo de empates usando criterios secundarios.
     */
    private fun sortEntriesWithTies(entries: List<LeaderboardEntry>): List<LeaderboardEntry> {
        return when (_state.value.sortBy) {
            SortBy.SCORE -> entries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.totalScore }
                    .thenByDescending { it.accuracy }
                    .thenByDescending { it.examsCompleted }
            )
            SortBy.ACCURACY -> entries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.accuracy }
                    .thenByDescending { it.totalScore }
                    .thenByDescending { it.examsCompleted }
            )
            SortBy.EXAMS -> entries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.examsCompleted }
                    .thenByDescending { it.totalScore }
                    .thenByDescending { it.accuracy }
            )
        }
    }

    private fun updateUserPosition(sortedEntries: List<LeaderboardEntry> = _state.value.entries) {
        val currentUid = _state.value.currentUid ?: return
        val position = sortedEntries.indexOfFirst { it.uid == currentUid }.let { index ->
            if (index >= 0) index + 1 else 0
        }
        
        if (position > 0) {
            val previousPosition = _state.value.userStats?.position
            _state.update {
                it.copy(
                    userStats = it.userStats?.copy(
                        position = position,
                        previousPosition = previousPosition
                    )
                )
            }
        }
    }

    private fun resetPagination() {
        hasMorePages = true
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}

data class RankingState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false, // Para pull-to-refresh manual
    val error: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val currentTab: RankingTab = RankingTab.NATIONAL,
    val schoolCode: String = "",
    val userUgelCode: String = "",
    val currentUid: String? = null,
    val userStats: UserRankingStats? = null,
    val isUserVisible: Boolean = false,
    val userEntry: LeaderboardEntry? = null,
    val userDisplayName: String? = null,
    val userPhotoUrl: String? = null,
    val userSelectedCosmeticId: String? = null,
    val sortBy: SortBy = SortBy.SCORE
)
