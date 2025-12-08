package com.eduquiz.data.repository

import com.eduquiz.data.db.AchievementsDao
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.StoreDao
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.DailyStreak
import com.eduquiz.domain.profile.InventoryItem
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val storeDao: StoreDao,
    private val achievementsDao: AchievementsDao,
) : ProfileRepository {

    override fun observeProfile(uid: String): Flow<UserProfile?> =
        profileDao.observeProfile(uid).map { it?.toDomain() }

    override suspend fun upsertProfile(profile: UserProfile) {
        profileDao.upsertProfile(profile.toEntity())
    }

    override suspend fun updateCoins(uid: String, delta: Int, updatedAtLocal: Long, syncState: String) {
        profileDao.updateCoins(uid, delta, updatedAtLocal, syncState)
    }

    override suspend fun addCoins(uid: String, delta: Int, reason: String, updatedAtLocal: Long, syncState: String) {
        // Por ahora, addCoins es igual a updateCoins (la razón se puede usar para logging futuro)
        // TODO: En el futuro, se puede crear una tabla de transacciones para rastrear el reason
        profileDao.updateCoins(uid, delta, updatedAtLocal, syncState)
    }

    override suspend fun updateSelectedCosmetic(
        uid: String,
        cosmeticId: String,
        updatedAtLocal: Long,
        syncState: String
    ) {
        profileDao.updateSelectedCosmetic(uid, cosmeticId, updatedAtLocal, syncState)
    }

    override suspend fun saveDailyStreak(streak: DailyStreak) {
        profileDao.upsertDailyStreak(streak.toEntity())
    }

    override fun observeDailyStreak(uid: String): Flow<DailyStreak?> =
        profileDao.observeDailyStreak(uid).map { it?.toDomain() }

    override suspend fun addInventoryItem(item: InventoryItem) {
        storeDao.insertInventoryItem(item.toEntity())
    }

    override suspend fun hasInventoryItem(uid: String, cosmeticId: String): Boolean =
        storeDao.hasInventoryItem(uid, cosmeticId)

    override suspend fun getInventory(uid: String): List<InventoryItem> =
        storeDao.getInventory(uid).map { it.toDomain() }

    override suspend fun unlockAchievement(achievement: Achievement) {
        achievementsDao.insertAchievement(achievement.toEntity())
    }

    override suspend fun getAchievements(uid: String): List<Achievement> =
        achievementsDao.getAchievements(uid).map { it.toDomain() }
}
