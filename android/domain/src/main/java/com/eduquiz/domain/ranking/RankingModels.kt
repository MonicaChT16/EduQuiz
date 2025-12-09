package com.eduquiz.domain.ranking

data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val totalScore: Int,
)

