package com.eduquiz.domain.sync

interface SyncRepository {
    suspend fun enqueueSync()
}
