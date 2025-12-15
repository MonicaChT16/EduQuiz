package com.eduquiz.domain.ranking

import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    /**
     * Observa el leaderboard del aula (top N) para un schoolId/classroomId.
     * Retorna un Flow de RankingResult que puede contener datos o errores.
     */
    fun observeClassroomLeaderboard(
        schoolId: String,
        classroomId: String
    ): Flow<RankingResult<List<LeaderboardEntry>>>
    
    /**
     * Observa el leaderboard de un colegio por su código (school_code).
     * Busca usuarios que tengan ese school_code en su perfil.
     * Retorna un Flow de RankingResult que puede contener datos o errores.
     * @deprecated Use loadSchoolLeaderboard instead for static data
     */
    fun observeSchoolLeaderboard(schoolCode: String): Flow<RankingResult<List<LeaderboardEntry>>>
    
    /**
     * Carga el leaderboard de un colegio de forma estática (una sola vez, no observa cambios).
     * Busca usuarios que tengan ese school_code en su perfil.
     */
    suspend fun loadSchoolLeaderboard(schoolCode: String): RankingResult<List<LeaderboardEntry>>
    
    /**
     * Observa el leaderboard nacional (todos los usuarios ordenados por puntaje descendente).
     * Retorna un Flow de RankingResult que puede contener datos o errores.
     * @deprecated Use loadNationalLeaderboard instead for static data
     */
    fun observeNationalLeaderboard(): Flow<RankingResult<List<LeaderboardEntry>>>
    
    /**
     * Carga el leaderboard nacional de forma estática (una sola vez, no observa cambios).
     */
    suspend fun loadNationalLeaderboard(): RankingResult<List<LeaderboardEntry>>
    
    /**
     * Carga más resultados para paginación (siguiente página).
     * @param schoolCode Código del colegio (null para ranking nacional)
     * @param lastScore Último score de la página anterior
     * @return Lista de entradas o error
     */
    suspend fun loadMoreSchoolLeaderboard(
        schoolCode: String,
        lastScore: Int
    ): RankingResult<List<LeaderboardEntry>>
    
    /**
     * Carga más resultados para ranking nacional (siguiente página).
     */
    suspend fun loadMoreNationalLeaderboard(
        lastScore: Int
    ): RankingResult<List<LeaderboardEntry>>
    
    /**
     * Calcula la posición real del usuario en el ranking.
     * Útil cuando el usuario no está en el top 100.
     * @param uid ID del usuario
     * @param schoolCode Código del colegio (null para ranking nacional)
     * @return Posición del usuario (1-based) o error
     */
    suspend fun calculateUserPosition(
        uid: String,
        schoolCode: String?
    ): RankingResult<Int>
}


