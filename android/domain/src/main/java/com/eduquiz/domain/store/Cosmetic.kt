package com.eduquiz.domain.store

/**
 * Modelo de dominio para un cosmético.
 * Los cosméticos funcionan como overlays/decoraciones sobre la foto de perfil (estilo Discord).
 */
data class Cosmetic(
    val cosmeticId: String,
    val name: String,
    val cost: Int, // Costo en EduCoins
    val description: String? = null,
    /**
     * URL de la imagen del overlay que se superpone sobre la foto de perfil.
     * Si es null, no hay overlay (cosmético desactivado).
     */
    val overlayImageUrl: String? = null
)

