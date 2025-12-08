package com.eduquiz.domain.user

data class EduQuizUser(
    val id: String,
    val fullName: String,
    val email: String,
)

fun interface GetCurrentUserUseCase {
    suspend operator fun invoke(): EduQuizUser?
}
