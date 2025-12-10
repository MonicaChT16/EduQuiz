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

    /**
     * Programa la verificación y actualización automática de packs.
     * Se ejecuta periódicamente cuando hay conexión a internet.
     */
    fun schedulePackUpdate()

    /**
     * Ejecuta una verificación inmediata de packs disponibles.
     * Útil cuando la app se inicia o cuando se detecta conexión a internet.
     */
    fun checkPackUpdateNow()
}
