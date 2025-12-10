package com.eduquiz.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun upsertProfile(profile: UserProfile)
    suspend fun updateCoins(uid: String, delta: Int, updatedAtLocal: Long, syncState: String)
    /**
     * Agrega coins al perfil con una razón específica.
     * @param reason Razón por la que se otorgan los coins (ej: "correct_answer", "speed_bonus", "streak_bonus")
     */
    suspend fun addCoins(uid: String, delta: Int, reason: String, updatedAtLocal: Long, syncState: String)
    /**
     * Agrega XP al perfil. La XP es acumulativa y nunca disminuye.
     * @param delta Cantidad de XP a agregar (siempre positivo)
     */
    suspend fun addXp(uid: String, delta: Long, updatedAtLocal: Long, syncState: String)
    suspend fun updateSelectedCosmetic(uid: String, cosmeticId: String?, updatedAtLocal: Long, syncState: String)
    
    /**
     * Actualiza la URL de la foto de perfil del usuario.
     */
    suspend fun updatePhotoUrl(uid: String, photoUrl: String?, updatedAtLocal: Long, syncState: String)
    
    /**
     * Actualiza el código UGEL del usuario.
     * Este código se usa para agrupar usuarios en rankings por colegio/UGEL.
     */
    suspend fun updateUgelCode(uid: String, ugelCode: String?, updatedAtLocal: Long, syncState: String)
    
    /**
     * Obtiene el perfil del usuario desde Firestore si no existe localmente.
     * Útil para recuperar datos después de desinstalar/reinstalar la app.
     */
    suspend fun fetchProfileFromFirestore(uid: String): UserProfile?

    suspend fun saveDailyStreak(streak: DailyStreak)
    fun observeDailyStreak(uid: String): Flow<DailyStreak?>

    suspend fun addInventoryItem(item: InventoryItem)
    suspend fun hasInventoryItem(uid: String, cosmeticId: String): Boolean
    suspend fun getInventory(uid: String): List<InventoryItem>

    suspend fun unlockAchievement(achievement: Achievement)
    suspend fun getAchievements(uid: String): List<Achievement>
}
