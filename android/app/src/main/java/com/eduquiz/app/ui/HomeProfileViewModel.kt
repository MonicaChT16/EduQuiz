package com.eduquiz.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.UserProfile
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.store.StoreRepository
import com.eduquiz.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.System

@HiltViewModel
class HomeProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    val profile: Flow<UserProfile?> = authRepository.authState.flatMapLatest { user ->
        if (user != null) {
            profileRepository.observeProfile(user.uid)
        } else {
            flowOf(null)
        }
    }

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            profile.map { it?.notificationsEnabled ?: true }.collect {
                _notificationsEnabled.value = it
            }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val user = authRepository.authState.first()
            user?.uid?.let { uid ->
                profileRepository.updateNotificationsEnabled(
                    uid = uid,
                    notificationsEnabled = enabled,
                    updatedAtLocal = System.currentTimeMillis(),
                    syncState = SyncState.PENDING
                )
                _notificationsEnabled.value = enabled
            }
        }
    }

    /**
     * Obtiene la URL/ref del overlay del cosmético desde el catálogo.
     */
    suspend fun getCosmeticOverlayUrl(cosmeticId: String): String? {
        return try {
            val catalog = storeRepository.getCatalog()
            catalog.find { it.cosmeticId == cosmeticId }?.overlayImageUrl
        } catch (e: Exception) {
            android.util.Log.e("HomeProfileViewModel", "Error getting cosmetic overlay URL", e)
            null
        }
    }
}
