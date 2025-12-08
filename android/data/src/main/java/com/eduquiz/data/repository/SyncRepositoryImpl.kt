package com.eduquiz.data.repository

import com.eduquiz.domain.sync.SyncRepository
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor() : SyncRepository {
    override suspend fun enqueueSync() {
        // Placeholder for WorkManager-based sync orchestration.
    }
}
