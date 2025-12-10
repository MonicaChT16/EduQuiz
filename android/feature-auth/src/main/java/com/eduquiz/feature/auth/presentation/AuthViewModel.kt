package com.eduquiz.feature.auth.presentation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.achievements.AchievementEngine
import com.eduquiz.domain.achievements.AchievementEvent
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.profile.UserProfile
import com.eduquiz.domain.streak.StreakService
import com.eduquiz.domain.sync.SyncRepository
import com.eduquiz.feature.auth.data.AuthRepository
import com.eduquiz.feature.auth.data.AuthResult
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.model.AuthStateReducer
import com.eduquiz.feature.auth.model.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val streakService: StreakService,
    private val achievementEngine: AchievementEngine,
    private val profileRepository: ProfileRepository,
    private val packRepository: PackRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private var lastKnownUser: AuthUser? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                lastKnownUser = user
                _state.update { current ->
                    if (current is AuthState.Error) current else AuthStateReducer.reduce(user)
                }
                
                // Crear o actualizar perfil y luego actualizar racha y evaluar logros
                if (user != null) {
                    try {
                        // Asegurar que el perfil exista
                        val existingProfile = profileRepository.observeProfile(user.uid).firstOrNull()
                        if (existingProfile == null) {
                            // Intentar obtener el perfil desde Firestore primero (por si desinstaló la app)
                            val firestoreProfile = profileRepository.fetchProfileFromFirestore(user.uid)
                            
                            if (firestoreProfile == null) {
                                // Si no existe en Firestore, crear perfil inicial
                                profileRepository.upsertProfile(
                                    UserProfile(
                                        uid = user.uid,
                                        displayName = user.displayName ?: "Usuario",
                                        photoUrl = user.photoUrl,
                                        schoolId = "",
                                        classroomId = "",
                                        ugelCode = null,
                                        coins = 0,
                                        xp = 0L,
                                        selectedCosmeticId = null,
                                        updatedAtLocal = System.currentTimeMillis(),
                                        syncState = SyncState.PENDING
                                    )
                                )
                            } else {
                                // El perfil se recuperó desde Firestore y ya está guardado en Room
                                android.util.Log.d("AuthViewModel", "Profile recovered from Firestore: ${firestoreProfile.ugelCode}")
                            }
                        }
                        
                        val updatedStreak = streakService.updateStreak(user.uid)
                        // Evaluar logros relacionados con racha
                        achievementEngine.evaluateAndUnlock(
                            uid = user.uid,
                            event = AchievementEvent.StreakUpdated(updatedStreak.currentStreak)
                        )
                        // Evaluar logros relacionados con login
                        achievementEngine.evaluateAndUnlock(
                            uid = user.uid,
                            event = AchievementEvent.Login
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Error updating profile, streak or achievements", e)
                        // No bloquear el flujo si hay un error
                    }
                }
            }
        }
    }

    fun launchGoogleSignIn(launchIntent: (Intent) -> Unit) {
        val intent = authRepository.getGoogleSignInIntent()
        if (intent == null) {
            _state.value = AuthState.Error(MISSING_CONFIG_MESSAGE)
        } else {
            launchIntent(intent)
        }
    }

    fun onGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            when (val result = authRepository.signInWithGoogleIntent(data)) {
                is AuthResult.Success -> {
                    _state.value = AuthState.Authenticated(result.user)
                    // Programar sincronización periódica al iniciar sesión
                    syncRepository.schedulePeriodicSync()
                    // También encolar sincronización inmediata para sincronizar datos pendientes
                    syncRepository.enqueueSyncNow()
                    // Descargar automáticamente el pack si no existe
                    downloadPackIfNeeded()
                }
                is AuthResult.Failure -> _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun onGoogleSignInCanceled() {
        _state.value = AuthState.Unauthenticated
    }

    /**
     * Descarga automáticamente el pack actual si no existe uno activo.
     * Esto asegura que el usuario tenga contenido disponible después de reinstalar la app.
     */
    private fun downloadPackIfNeeded() {
        viewModelScope.launch {
            try {
                // Verificar si hay un pack activo
                val activePack = packRepository.observeActivePack().firstOrNull()
                if (activePack == null) {
                    // No hay pack activo, intentar descargar el pack actual
                    val availablePack = packRepository.fetchCurrentPackMeta()
                    if (availablePack != null) {
                        android.util.Log.d("AuthViewModel", "Auto-downloading pack: ${availablePack.packId}")
                        packRepository.downloadPack(availablePack.packId)
                        android.util.Log.d("AuthViewModel", "Pack downloaded successfully")
                    } else {
                        android.util.Log.d("AuthViewModel", "No pack available to download")
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "Pack already active: ${activePack.packId}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error auto-downloading pack", e)
                // No bloquear el flujo si falla la descarga del pack
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            runCatching { authRepository.signOut() }
                .onFailure { throwable ->
                    val fallbackUser = lastKnownUser
                    if (fallbackUser != null) {
                        _state.value = AuthState.Authenticated(fallbackUser)
                    } else {
                        _state.value = AuthState.Error(
                            throwable.localizedMessage ?: DEFAULT_LOGOUT_ERROR
                        )
                    }
                }
        }
    }

    companion object {
        private const val MISSING_CONFIG_MESSAGE =
            "Copia google-services.json en android/app y vuelve a intentar."
        private const val DEFAULT_LOGOUT_ERROR = "No se pudo cerrar sesion."
    }
}
