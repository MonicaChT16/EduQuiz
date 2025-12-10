package com.eduquiz.domain.ranking

import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    /**
        * Observa el leaderboard del aula (top N) para un schoolId/classroomId.
        */
    fun observeClassroomLeaderboard(
        schoolId: String,
        classroomId: String
    ): Flow<List<LeaderboardEntry>>
    
    /**
     * Observa el leaderboard de un colegio por su código (school_code).
     * Busca usuarios que tengan ese school_code en su perfil.
     */
    fun observeSchoolLeaderboard(schoolCode: String): Flow<List<LeaderboardEntry>>
    
    /**
     * Observa el leaderboard nacional (todos los usuarios ordenados por puntaje descendente).
     */
    fun observeNationalLeaderboard(): Flow<List<LeaderboardEntry>>
}


