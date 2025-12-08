package com.eduquiz.domain.store

/**
 * Modelo de dominio para un cosmético.
 */
data class Cosmetic(
    val cosmeticId: String,
    val name: String,
    val cost: Int, // Costo en EduCoins
    val description: String? = null
)

