package com.eduquiz.domain.pack

data class Pack(
    val packId: String,
    val weekLabel: String,
    val status: String,
    val publishedAt: Long,
    val downloadedAt: Long,
)

data class TextContent(
    val textId: String,
    val packId: String,
    val title: String,
    val body: String,
    val subject: String,
)

data class Question(
    val questionId: String,
    val packId: String,
    val textId: String,
    val prompt: String,
    val correctOptionId: String,
    val difficulty: Int,
    val explanationText: String?,
    val explanationStatus: String,
)

data class Option(
    val questionId: String,
    val optionId: String,
    val text: String,
)

object PackStatus {
    const val DOWNLOADED = "DOWNLOADED"
    const val ACTIVE = "ACTIVE"
    const val ARCHIVED = "ARCHIVED"
}

object ExplanationStatus {
    const val NONE = "NONE"
    const val GENERATED = "GENERATED"
    const val APPROVED = "APPROVED"
}
