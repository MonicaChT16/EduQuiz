package com.eduquiz.data.repository

import com.eduquiz.domain.store.Cosmetic

object CosmeticCatalog {
    val COSMETICS = listOf(
        Cosmetic(
            cosmeticId = "cosmetic_1",
            name = "Corona Dorada",
            cost = 50,
            description = "Una corona dorada elegante",
            overlayImageUrl = null
        ),
        Cosmetic(
            cosmeticId = "cosmetic_2",
            name = "Sombrero de Magia",
            cost = 100,
            description = "Un sombrero mágico",
            overlayImageUrl = null
        ),
        Cosmetic(
            cosmeticId = "cosmetic_3",
            name = "Gafas de Sol",
            cost = 25,
            description = "Gafas de sol estilosas",
            overlayImageUrl = null
        ),
        Cosmetic(
            cosmeticId = "cosmetic_4",
            name = "Máscara de Héroe",
            cost = 150,
            description = "Máscara de superhéroe",
            overlayImageUrl = null
        ),
        Cosmetic(
            cosmeticId = "cosmetic_5",
            name = "Aureola",
            cost = 200,
            description = "Una aureola brillante",
            overlayImageUrl = null
        )
    )
}







