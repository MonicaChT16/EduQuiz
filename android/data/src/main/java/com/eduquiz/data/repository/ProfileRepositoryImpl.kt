package com.eduquiz.data.repository

import com.eduquiz.data.db.AchievementsDao
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.StoreDao
import com.eduquiz.data.remote.FirestoreSyncService
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.DailyStreak
import com.eduquiz.domain.profile.InventoryItem
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.profile.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val storeDao: StoreDao,
    private val achievementsDao: AchievementsDao,
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    override fun observeProfile(uid: String): Flow<UserProfile?> =
        profileDao.observeProfile(uid).map { it?.toDomain() }

    override suspend fun upsertProfile(profile: UserProfile) {
        profileDao.upsertProfile(profile.toEntity())
    }

    override suspend fun updateCoins(uid: String, delta: Int, updatedAtLocal: Long, syncState: String) {
        // Verificar que el perfil exista antes de actualizar
        val profile = profileDao.observeProfile(uid).firstOrNull()
        if (profile == null) {
            android.util.Log.w("ProfileRepository", "Profile not found for $uid, cannot update coins")
            return
        }
        profileDao.updateCoins(uid, delta, updatedAtLocal, syncState)
    }

    override suspend fun addCoins(uid: String, delta: Int, reason: String, updatedAtLocal: Long, syncState: String) {
        // Por ahora, addCoins es igual a updateCoins (la razón se puede usar para logging futuro)
        // TODO: En el futuro, se puede crear una tabla de transacciones para rastrear el reason
        // Verificar que el perfil exista antes de actualizar
        val profile = profileDao.observeProfile(uid).firstOrNull()
        if (profile == null) {
            android.util.Log.w("ProfileRepository", "Profile not found for $uid, cannot add coins")
            return
        }
        profileDao.updateCoins(uid, delta, updatedAtLocal, syncState)
    }

    override suspend fun addXp(uid: String, delta: Long, updatedAtLocal: Long, syncState: String) {
        // Verificar que el perfil exista antes de actualizar
        val profile = profileDao.observeProfile(uid).firstOrNull()
        if (profile == null) {
            android.util.Log.w("ProfileRepository", "Profile not found for $uid, cannot add XP")
            return
        }
        // XP siempre se suma (nunca disminuye)
        profileDao.updateXp(uid, delta, updatedAtLocal, syncState)
    }

    override suspend fun updateSelectedCosmetic(
        uid: String,
        cosmeticId: String?,
        updatedAtLocal: Long,
        syncState: String
    ) {
        profileDao.updateSelectedCosmetic(uid, cosmeticId, updatedAtLocal, syncState)
    }

    override suspend fun updatePhotoUrl(uid: String, photoUrl: String?, updatedAtLocal: Long, syncState: String) {
        profileDao.updatePhotoUrl(uid, photoUrl, updatedAtLocal, syncState)
    }

    override suspend fun updateUgelCode(uid: String, ugelCode: String?, updatedAtLocal: Long, syncState: String) {
        profileDao.updateUgelCode(uid, ugelCode, updatedAtLocal, syncState)
    }

    override suspend fun fetchProfileFromFirestore(uid: String): UserProfile? {
        return try {
            val profileRef = firestore.collection("users").document(uid)
            val snapshot = profileRef.get().await()
            
            if (!snapshot.exists()) {
                android.util.Log.d("ProfileRepository", "Profile not found in Firestore for $uid")
                return null
            }
            
            // Mapear desde Firestore a UserProfile
            // Priorizar ugelCode sobre schoolCode (el código UGEL es más específico)
            val ugelCode = snapshot.getString("ugelCode")?.takeIf { it.isNotBlank() }
            val schoolCode = snapshot.getString("schoolCode")?.takeIf { it.isNotBlank() }
            // Usar ugelCode si existe, sino schoolCode (compatibilidad con datos antiguos)
            val finalUgelCode = ugelCode ?: schoolCode
            
            // Preservar el código UGEL si existe (no se borra)
            val profile = UserProfile(
                uid = snapshot.id,
                displayName = snapshot.getString("displayName") ?: "Usuario",
                photoUrl = snapshot.getString("photoUrl"),
                schoolId = "", // Legacy - no se usa
                classroomId = "", // Legacy - no se usa
                ugelCode = finalUgelCode, // Preservar el código UGEL desde Firestore
                coins = (snapshot.getLong("coins") ?: 0L).toInt(),
                xp = snapshot.getLong("xp") ?: snapshot.getLong("totalXp") ?: 0L,
                selectedCosmeticId = snapshot.getString("selectedCosmeticId"),
                updatedAtLocal = snapshot.getLong("updatedAtLocal") ?: System.currentTimeMillis(),
                syncState = SyncState.SYNCED,
                notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true
            )
            
            // Guardar en Room
            profileDao.upsertProfile(profile.toEntity())
            android.util.Log.d("ProfileRepository", "Profile fetched from Firestore and saved locally: $uid")
            
            profile
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "Error fetching profile from Firestore", e)
            null
        }
    }

    override suspend fun updateNotificationsEnabled(
        uid: String,
        notificationsEnabled: Boolean,
        updatedAtLocal: Long,
        syncState: String
    ) {
        profileDao.updateNotificationsEnabled(uid, notificationsEnabled, updatedAtLocal, syncState)
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
