package com.eduquiz.domain.profile

data class UserProfile(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val schoolId: String,
    val classroomId: String,
    val coins: Int,
    val selectedCosmeticId: String?, // Nullable porque puede no tener cosmético equipado
    val updatedAtLocal: Long,
    val syncState: String,
)

data class InventoryItem(
    val uid: String,
    val cosmeticId: String,
    val purchasedAt: Long,
)

data class Achievement(
    val uid: String,
    val achievementId: String,
    val unlockedAt: Long,
)

data class DailyStreak(
    val uid: String,
    val currentStreak: Int,
    val lastLoginDate: String,
    val updatedAtLocal: Long,
    val syncState: String,
)

object SyncState {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}
