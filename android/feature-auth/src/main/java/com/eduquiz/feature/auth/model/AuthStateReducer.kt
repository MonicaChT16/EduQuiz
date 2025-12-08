package com.eduquiz.feature.auth.model

internal object AuthStateReducer {
    fun reduce(user: AuthUser?): AuthState {
        return user?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    }
}
