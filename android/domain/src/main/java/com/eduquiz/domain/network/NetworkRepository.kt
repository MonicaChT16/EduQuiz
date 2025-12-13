package com.eduquiz.domain.network

interface NetworkRepository {
    suspend fun isConnected(): Boolean
}

