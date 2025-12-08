package com.eduquiz.feature.auth.data

import android.content.Intent
import com.eduquiz.feature.auth.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleClientProvider: GoogleSignInClientProvider
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toAuthUser())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override fun getGoogleSignInIntent(): Intent? {
        return googleClientProvider.getClient()?.signInIntent
    }

    override suspend fun signInWithGoogleIntent(data: Intent?): AuthResult {
        val idToken = googleClientProvider.extractIdToken(data)
            ?: return AuthResult.Failure("No se recibieron credenciales de Google.")

        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user?.toAuthUser()
                ?: error("No se pudo crear la sesion de Firebase.")
            AuthResult.Success(user)
        }.getOrElse { throwable ->
            AuthResult.Failure(throwable.localizedMessage ?: "Error al iniciar sesion con Firebase.")
        }
    }

    override suspend fun signOut() {
        runCatching {
            googleClientProvider.getClient()?.signOut()?.await()
        }
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl?.toString()
        )
    }
}
