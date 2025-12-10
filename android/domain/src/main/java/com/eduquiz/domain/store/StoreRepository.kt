package com.eduquiz.domain.store

/**
 * Repositorio para operaciones de la tienda.
 */
interface StoreRepository {
    /**
     * Obtiene el catálogo completo de cosméticos disponibles.
     */
    suspend fun getCatalog(): List<Cosmetic>
    
    /**
     * Compra un cosmético si el usuario tiene suficientes coins.
     * @return true si la compra fue exitosa, false si no hay suficientes coins o ya está comprado
     */
    suspend fun purchaseCosmetic(uid: String, cosmeticId: String): Boolean
    
    /**
     * Equipa un cosmético (solo si ya está comprado).
     * @return true si se equipó exitosamente, false si no está comprado
     */
    suspend fun equipCosmetic(uid: String, cosmeticId: String): Boolean
    
    /**
     * Verifica si un cosmético está comprado.
     */
    suspend fun isCosmeticPurchased(uid: String, cosmeticId: String): Boolean
}













