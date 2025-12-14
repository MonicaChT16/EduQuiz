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
 * Los cosméticos son overlays que se aplican sobre la foto de perfil.
 * Por ahora usamos URLs de ejemplo - en producción estas vendrían de Firebase Storage o un CDN.
 */
object CosmeticCatalog {
    val COSMETICS = listOf(
        Cosmetic(
            cosmeticId = "basic_frame",
            name = "Marco Básico",
            cost = 0, // Gratis - cosmético inicial
            description = "Un marco simple y elegante para tu foto de perfil",
            overlayImageUrl = null // Se renderiza como borde simple en la UI
        ),
        Cosmetic(
            cosmeticId = "crown_gold",
            name = "Corona Dorada",
            cost = 100,
            description = "Una elegante corona dorada sobre tu foto",
            overlayImageUrl = "drawable://corona_dorada"
        ),
        Cosmetic(
            cosmeticId = "star_gold",
            name = "Estrella Dorada",
            cost = 150,
            description = "Una estrella brillante en tu foto",
            overlayImageUrl = "drawable://estrella_dorada"
        ),
        Cosmetic(
            cosmeticId = "badge_champion",
            name = "Insignia de Campeón",
            cost = 200,
            description = "Insignia de campeón académico",
            overlayImageUrl = "drawable://insignia_campeon"
        ),
        Cosmetic(
            cosmeticId = "halo_gold",
            name = "Aureola Dorada",
            cost = 300,
            description = "Una aureola brillante alrededor de tu foto",
            overlayImageUrl = "drawable://aureola_dorada"
        ),
        Cosmetic(
            cosmeticId = "frame_gold",
            name = "Marco Dorado",
            cost = 250,
            description = "Un marco elegante alrededor de tu foto",
            overlayImageUrl = "drawable://marco_dorado"
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
