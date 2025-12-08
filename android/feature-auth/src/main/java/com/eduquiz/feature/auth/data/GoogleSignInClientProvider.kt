package com.eduquiz.feature.auth.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInClientProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cachedClient: GoogleSignInClient? = null

    fun getClient(): GoogleSignInClient? {
        if (cachedClient == null) {
            cachedClient = buildClientOrNull()
        }
        return cachedClient
    }

    fun extractIdToken(data: Intent?): String? {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)?.idToken
        }.onFailure { throwable ->
            Log.w(TAG, "No se pudo obtener el token de Google Sign-In", throwable)
        }.getOrNull()
    }

    private fun buildClientOrNull(): GoogleSignInClient? {
        val clientId = resolveDefaultWebClientId()
        if (clientId.isNullOrBlank()) {
            Log.w(TAG, "default_web_client_id no encontrado. Copia google-services.json en android/app.")
            return null
        }
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    private fun resolveDefaultWebClientId(): String? {
        val identifier = context.resources.getIdentifier(
            DEFAULT_WEB_CLIENT_RES,
            "string",
            context.packageName
        )
        return if (identifier != 0) {
            context.getString(identifier)
        } else {
            null
        }
    }

    private companion object {
        private const val TAG = "GoogleClientProvider"
        private const val DEFAULT_WEB_CLIENT_RES = "default_web_client_id"
    }
}
