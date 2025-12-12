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
    
    /**
     * Sincroniza todos los usuarios de la app a Firestore.
     * Útil para migrar usuarios existentes o forzar una actualización masiva.
     * @return Resultado con el número de usuarios sincronizados y fallidos
     */
    suspend fun syncAllUsers(): SyncAllUsersResult
    
    /**
     * Encola una sincronización masiva de todos los usuarios en segundo plano.
     * Se ejecuta cuando hay conexión a internet.
     */
    fun enqueueSyncAllUsers()
    
    /**
     * Sincroniza inmediatamente el perfil de un usuario específico a Firestore.
     * Marca el perfil como PENDING y luego lo sincroniza directamente.
     * @param uid ID del usuario a sincronizar
     * @return true si la sincronización fue exitosa, false en caso contrario
     */
    suspend fun syncUserProfileNow(uid: String): Boolean
}

/**
 * Resultado de la sincronización masiva de usuarios.
 */
data class SyncAllUsersResult(
    val totalUsers: Int,
    val syncedUsers: Int,
    val failedUsers: Int,
    val skippedUsers: Int
)
