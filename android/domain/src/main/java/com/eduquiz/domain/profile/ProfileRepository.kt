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
    suspend fun updateSelectedCosmetic(uid: String, cosmeticId: String?, updatedAtLocal: Long, syncState: String)

    suspend fun saveDailyStreak(streak: DailyStreak)
    fun observeDailyStreak(uid: String): Flow<DailyStreak?>

    suspend fun addInventoryItem(item: InventoryItem)
    suspend fun hasInventoryItem(uid: String, cosmeticId: String): Boolean
    suspend fun getInventory(uid: String): List<InventoryItem>

    suspend fun unlockAchievement(achievement: Achievement)
    suspend fun getAchievements(uid: String): List<Achievement>
}
