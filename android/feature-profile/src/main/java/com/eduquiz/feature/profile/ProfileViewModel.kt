package com.eduquiz.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _currentUid = MutableStateFlow<String?>(null)
    
    val attempts: StateFlow<List<ExamAttempt>> = 
        _currentUid.asStateFlow()
            .flatMapLatest { uid ->
                if (uid != null) {
                    examRepository.observeAttempts(uid)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = emptyList()
            )

    val profile: StateFlow<UserProfile?> =
        _currentUid.asStateFlow()
            .flatMapLatest { uid ->
                if (uid != null) {
                    profileRepository.observeProfile(uid)
                } else {
                    flowOf(null)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = null
            )

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    fun initialize(uid: String) {
        if (_currentUid.value != uid) {
            _currentUid.value = uid
            // Cargar logros cuando se inicializa
            viewModelScope.launch {
                try {
                    _achievements.value = profileRepository.getAchievements(uid)
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error loading achievements", e)
                    _achievements.value = emptyList()
                }
            }
        }
    }
    
    fun refreshAchievements() {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            try {
                _achievements.value = profileRepository.getAchievements(uid)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error refreshing achievements", e)
            }
        }
    }
}
