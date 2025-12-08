package com.eduquiz.feature.auth.presentation

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduquiz.domain.sync.SyncRepository
import com.eduquiz.feature.auth.data.AuthRepository
import com.eduquiz.feature.auth.data.AuthResult
import com.eduquiz.feature.auth.model.AuthState
import com.eduquiz.feature.auth.model.AuthStateReducer
import com.eduquiz.feature.auth.model.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
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
                }
                is AuthResult.Failure -> _state.value = AuthState.Error(result.message)
            }
        }
    }

    fun onGoogleSignInCanceled() {
        _state.value = AuthState.Unauthenticated
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
