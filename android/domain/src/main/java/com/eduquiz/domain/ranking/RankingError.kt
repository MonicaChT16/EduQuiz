package com.eduquiz.domain.ranking

/**
 * Errores que pueden ocurrir al cargar rankings desde Firestore.
 */
sealed class RankingError {
    abstract val message: String
    
    /**
     * Error de red o conexión con Firestore.
     */
    data class NetworkError(
        override val message: String = "Error de conexión. Verifica tu internet."
    ) : RankingError()
    
    /**
     * Falta un índice compuesto en Firestore.
     * Ocurre cuando se intenta consultar por schoolCode sin el índice necesario.
     */
    data class IndexMissing(
        override val message: String = "Índice de Firestore faltante. Contacta al administrador."
    ) : RankingError()
    
    /**
     * Error de permisos en Firestore.
     */
    data class PermissionDenied(
        override val message: String = "No tienes permisos para ver este ranking."
    ) : RankingError()
    
    /**
     * Error genérico de Firestore con código y mensaje.
     */
    data class FirestoreError(
        val code: String,
        override val message: String
    ) : RankingError()
    
    /**
     * Código UGEL inválido o formato incorrecto.
     */
    data class InvalidSchoolCode(
        override val message: String = "Código UGEL inválido. Debe tener 7 dígitos numéricos."
    ) : RankingError()
    
    /**
     * No hay datos disponibles (ranking vacío).
     */
    object EmptyRanking : RankingError() {
        override val message: String = "No hay usuarios en este ranking aún."
    }
    
    /**
     * Error desconocido.
     */
    data class UnknownError(
        override val message: String = "Error desconocido al cargar el ranking."
    ) : RankingError()
}

