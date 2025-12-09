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
}

