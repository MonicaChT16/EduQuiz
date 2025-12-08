package com.eduquiz.feature.auth.data

import android.content.Intent
import com.eduquiz.feature.auth.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>

    fun getGoogleSignInIntent(): Intent?

    suspend fun signInWithGoogleIntent(data: Intent?): AuthResult

    suspend fun signOut()
}
