package com.eduquiz.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.data.storage.ImageStorageService
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.profile.UserProfile
import com.eduquiz.domain.profile.UserStats
import com.eduquiz.domain.store.StoreRepository
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
    private val profileRepository: ProfileRepository,
    private val imageStorageService: ImageStorageService,
    private val storeRepository: StoreRepository
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
    
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun initialize(uid: String) {
        if (_currentUid.value != uid) {
            _currentUid.value = uid
            viewModelScope.launch {
                try {
                    _achievements.value = profileRepository.getAchievements(uid)
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error loading achievements", e)
                    _achievements.value = emptyList()
                }
            }
            viewModelScope.launch {
                try {
                    _userStats.value = profileRepository.getUserStats(uid)
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "Error loading user stats", e)
                    _userStats.value = null
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
    
    /**
     * Sube una foto de perfil y actualiza el perfil con la nueva URL.
     */
    fun uploadProfilePhoto(imageUri: Uri) {
        val uid = _currentUid.value ?: return
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            try {
                // Subir imagen a Firebase Storage
                val photoUrl = imageStorageService.uploadProfileImage(uid, imageUri)
                
                // Actualizar perfil con la nueva URL
                profileRepository.updatePhotoUrl(
                    uid = uid,
                    photoUrl = photoUrl,
                    updatedAtLocal = System.currentTimeMillis(),
                    syncState = SyncState.PENDING
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error uploading profile photo", e)
                _uploadError.value = "Error al subir la foto: ${e.message ?: "Error desconocido"}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }
    
    /**
     * Obtiene la URL del overlay de un cosmético desde el catálogo.
     */
    suspend fun getCosmeticOverlayUrl(cosmeticId: String): String? {
        return try {
            val catalog = storeRepository.getCatalog()
            catalog.find { it.cosmeticId == cosmeticId }?.overlayImageUrl
        } catch (e: Exception) {
            android.util.Log.e("ProfileViewModel", "Error getting cosmetic overlay URL", e)
            null
        }
    }
}
