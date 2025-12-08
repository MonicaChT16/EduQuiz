package com.eduquiz.feature.auth.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthStateReducerTest {

    @Test
    fun `reduce returns authenticated when user is present`() {
        val user = AuthUser(
            uid = "uid-123",
            displayName = "Alex",
            email = "alex@example.com",
            photoUrl = null
        )

        val result = AuthStateReducer.reduce(user)

        assertEquals(AuthState.Authenticated(user), result)
    }

    @Test
    fun `reduce returns unauthenticated when user is missing`() {
        val result = AuthStateReducer.reduce(null)

        assertEquals(AuthState.Unauthenticated, result)
    }
}
