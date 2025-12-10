package com.eduquiz.domain.exam

data class ExamAttempt(
    val attemptId: String,
    val uid: String,
    val packId: String,
    val subject: String?,
    val startedAtLocal: Long,
    val finishedAtLocal: Long?,
    val durationMs: Long,
    val status: String,
    val scoreRaw: Int,
    val scoreValidated: Int?,
    val origin: String,
    val syncState: String,
)

data class ExamAnswer(
    val attemptId: String,
    val questionId: String,
    val selectedOptionId: String,
    val isCorrect: Boolean,
    val timeSpentMs: Long,
)

object ExamStatus {
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val AUTO_SUBMIT = "AUTO_SUBMIT"
    const val CANCELLED_CHEAT = "CANCELLED_CHEAT"
}

object ExamOrigin {
    const val OFFLINE = "OFFLINE"
    const val ONLINE = "ONLINE"
}
