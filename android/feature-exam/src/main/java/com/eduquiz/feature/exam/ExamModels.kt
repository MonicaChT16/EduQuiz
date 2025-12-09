package com.eduquiz.feature.exam

import com.eduquiz.domain.pack.Option
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackMeta
import com.eduquiz.domain.pack.Question
import com.eduquiz.domain.pack.TextContent

const val DEFAULT_EXAM_DURATION_MS = 20 * 60 * 1000L
const val OPTION_LOCK_MS = 5_000L

enum class ExamStage { Loading, Start, InProgress, Finished }

data class ExamContent(
    val question: Question,
    val text: TextContent,
    val options: List<Option>
)

data class ExamUiState(
    val stage: ExamStage = ExamStage.Loading,
    val pack: Pack? = null,
    val availablePack: PackMeta? = null,
    val attemptId: String? = null,
    val questions: List<ExamContent> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val durationMs: Long = DEFAULT_EXAM_DURATION_MS,
    val remainingMs: Long = DEFAULT_EXAM_DURATION_MS,
    val lockRemainingMs: Long = 0L,
    val showWarningDialog: Boolean = false,
    val leaveCount: Int = 0,
    val finishedStatus: String? = null,
    val correctCount: Int = 0,
    val errorMessage: String? = null,
    val isBusy: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoadingPack: Boolean = false,
)

val ExamUiState.totalQuestions: Int
    get() = questions.size

val ExamUiState.answeredCount: Int
    get() = answers.size

val ExamUiState.areOptionsLocked: Boolean
    get() = lockRemainingMs > 0

data class ExamResult(
    val attemptId: String,
    val status: String,
    val scoreRaw: Int,
    val answeredCount: Int,
    val finishedAtLocal: Long?,
    val totalQuestions: Int
)