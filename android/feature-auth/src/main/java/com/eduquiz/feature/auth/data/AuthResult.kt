package com.eduquiz.feature.auth.data

import com.eduquiz.feature.auth.model.AuthUser

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val message: String) : AuthResult
}
