package com.eduquiz.data.repository

import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.StoreDao
import com.eduquiz.domain.profile.SyncState
import com.eduquiz.domain.store.Cosmetic
import com.eduquiz.domain.store.StoreRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Implementación del catálogo de cosméticos (MVP hardcoded).
 */
object CosmeticCatalog {
    val COSMETICS = listOf(
        Cosmetic(
            cosmeticId = "cosmetic_1",
            name = "Avatar Clásico",
            cost = 0, // Gratis
            description = "Avatar predeterminado"
        ),
        Cosmetic(
            cosmeticId = "cosmetic_2",
            name = "Avatar Estudiante",
            cost = 50,
            description = "Avatar de estudiante dedicado"
        ),
        Cosmetic(
            cosmeticId = "cosmetic_3",
            name = "Avatar Profesor",
            cost = 100,
            description = "Avatar de profesor sabio"
        ),
        Cosmetic(
            cosmeticId = "cosmetic_4",
            name = "Avatar Campeón",
            cost = 200,
            description = "Avatar de campeón académico"
        ),
        Cosmetic(
            cosmeticId = "cosmetic_5",
            name = "Avatar Estrella",
            cost = 500,
            description = "Avatar de estrella brillante"
        )
    )
}

class StoreRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val storeDao: StoreDao
) : StoreRepository {

    override suspend fun getCatalog(): List<Cosmetic> = CosmeticCatalog.COSMETICS

    override suspend fun purchaseCosmetic(uid: String, cosmeticId: String): Boolean {
        // 1. Verificar que el cosmético existe en el catálogo
        val cosmetic = CosmeticCatalog.COSMETICS.find { it.cosmeticId == cosmeticId }
            ?: return false

        // 2. Verificar que no esté ya comprado
        if (storeDao.hasInventoryItem(uid, cosmeticId)) {
            return false // Ya está comprado
        }

        // 3. Obtener perfil actual
        val profile = profileDao.observeProfile(uid).firstOrNull()
            ?: return false

        // 4. Verificar que tenga suficientes coins
        if (profile.coins < cosmetic.cost) {
            return false // No hay suficientes coins
        }

        // 5. Descontar coins y agregar al inventario
        val updatedAtLocal = System.currentTimeMillis()

        // Actualizar coins (delta negativo)
        profileDao.updateCoins(uid, -cosmetic.cost, updatedAtLocal, SyncState.PENDING)

        // Agregar al inventario
        storeDao.insertInventoryItem(
            com.eduquiz.data.db.InventoryEntity(
                uid = uid,
                cosmeticId = cosmeticId,
                purchasedAt = updatedAtLocal
            )
        )

        return true
    }

    override suspend fun equipCosmetic(uid: String, cosmeticId: String): Boolean {
        // Verificar que esté comprado
        if (!storeDao.hasInventoryItem(uid, cosmeticId)) {
            return false
        }

        // Equipar (actualizar selectedCosmeticId)
        val updatedAtLocal = System.currentTimeMillis()
        profileDao.updateSelectedCosmetic(uid, cosmeticId, updatedAtLocal, SyncState.PENDING)
        return true
    }

    override suspend fun isCosmeticPurchased(uid: String, cosmeticId: String): Boolean =
        storeDao.hasInventoryItem(uid, cosmeticId)
}

