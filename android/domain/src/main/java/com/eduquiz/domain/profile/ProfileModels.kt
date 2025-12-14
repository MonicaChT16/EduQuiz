package com.eduquiz.domain.profile

data class UserProfile(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val schoolId: String,
    val classroomId: String,
    val ugelCode: String? = null, // Código UGEL ingresado por el usuario
    val coins: Int,
    val xp: Long = 0L, // Puntos de experiencia (acumulativo, nunca disminuye)
    val selectedCosmeticId: String?, // Nullable porque puede no tener cosmético equipado
    val updatedAtLocal: Long,
    val syncState: String,
    val notificationsEnabled: Boolean = true, // Nuevo campo para controlar las notificaciones
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
