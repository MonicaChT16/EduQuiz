package com.eduquiz.domain.sync

interface SyncRepository {
    /**
     * Encola una sincronización inmediata (OneTimeWorkRequest).
     * Útil cuando se completa un examen o se actualiza el perfil.
     */
    suspend fun enqueueSyncNow()

    /**
     * Programa una sincronización periódica (PeriodicWorkRequest).
     * Útil para sincronizar periódicamente cuando hay internet.
     */
    fun schedulePeriodicSync()
}
