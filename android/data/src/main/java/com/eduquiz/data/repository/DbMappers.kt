package com.eduquiz.data.repository

import com.eduquiz.data.db.AchievementEntity
import com.eduquiz.data.db.DailyStreakEntity
import com.eduquiz.data.db.ExamAnswerEntity
import com.eduquiz.data.db.ExamAttemptEntity
import com.eduquiz.data.db.InventoryEntity
import com.eduquiz.data.db.OptionEntity
import com.eduquiz.data.db.PackEntity
import com.eduquiz.data.db.QuestionEntity
import com.eduquiz.data.db.TextEntity
import com.eduquiz.data.db.UserProfileEntity
import com.eduquiz.domain.exam.ExamAnswer
import com.eduquiz.domain.exam.ExamAttempt
import com.eduquiz.domain.pack.Option
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.Question
import com.eduquiz.domain.pack.TextContent
import com.eduquiz.domain.profile.Achievement
import com.eduquiz.domain.profile.DailyStreak
import com.eduquiz.domain.profile.InventoryItem
import com.eduquiz.domain.profile.UserProfile

fun PackEntity.toDomain() = Pack(
    packId = packId,
    weekLabel = weekLabel,
    status = status,
    publishedAt = publishedAt,
    downloadedAt = downloadedAt,
)

fun Pack.toEntity() = PackEntity(
    packId = packId,
    weekLabel = weekLabel,
    status = status,
    publishedAt = publishedAt,
    downloadedAt = downloadedAt,
)

fun TextEntity.toDomain() = TextContent(
    textId = textId,
    packId = packId,
    title = title,
    body = body,
    subject = subject,
)

fun TextContent.toEntity() = TextEntity(
    textId = textId,
    packId = packId,
    title = title,
    body = body,
    subject = subject,
)

fun QuestionEntity.toDomain() = Question(
    questionId = questionId,
    packId = packId,
    textId = textId,
    prompt = prompt,
    correctOptionId = correctOptionId,
    difficulty = difficulty,
    explanationText = explanationText,
    explanationStatus = explanationStatus,
)

fun Question.toEntity() = QuestionEntity(
    questionId = questionId,
    packId = packId,
    textId = textId,
    prompt = prompt,
    correctOptionId = correctOptionId,
    difficulty = difficulty,
    explanationText = explanationText,
    explanationStatus = explanationStatus,
)

fun OptionEntity.toDomain() = Option(
    questionId = questionId,
    optionId = optionId,
    text = text,
)

fun Option.toEntity() = OptionEntity(
    questionId = questionId,
    optionId = optionId,
    text = text,
)

fun UserProfileEntity.toDomain() = UserProfile(
    uid = uid,
    displayName = displayName,
    photoUrl = photoUrl,
    schoolId = schoolId,
    classroomId = classroomId,
    ugelCode = ugelCode,
    coins = coins,
    xp = xp,
    selectedCosmeticId = selectedCosmeticId,
    updatedAtLocal = updatedAtLocal,
    syncState = syncState,
)

fun UserProfile.toEntity() = UserProfileEntity(
    uid = uid,
    displayName = displayName,
    photoUrl = photoUrl,
    schoolId = schoolId,
    classroomId = classroomId,
    ugelCode = ugelCode,
    coins = coins,
    xp = xp,
    selectedCosmeticId = selectedCosmeticId,
    updatedAtLocal = updatedAtLocal,
    syncState = syncState,
)

fun InventoryEntity.toDomain() = InventoryItem(
    uid = uid,
    cosmeticId = cosmeticId,
    purchasedAt = purchasedAt,
)

fun InventoryItem.toEntity() = InventoryEntity(
    uid = uid,
    cosmeticId = cosmeticId,
    purchasedAt = purchasedAt,
)

fun AchievementEntity.toDomain() = Achievement(
    uid = uid,
    achievementId = achievementId,
    unlockedAt = unlockedAt,
)

fun Achievement.toEntity() = AchievementEntity(
    uid = uid,
    achievementId = achievementId,
    unlockedAt = unlockedAt,
)

fun DailyStreakEntity.toDomain() = DailyStreak(
    uid = uid,
    currentStreak = currentStreak,
    lastLoginDate = lastLoginDate,
    updatedAtLocal = updatedAtLocal,
    syncState = syncState,
)

fun DailyStreak.toEntity() = DailyStreakEntity(
    uid = uid,
    currentStreak = currentStreak,
    lastLoginDate = lastLoginDate,
    updatedAtLocal = updatedAtLocal,
    syncState = syncState,
)

fun ExamAttemptEntity.toDomain() = ExamAttempt(
    attemptId = attemptId,
    uid = uid,
    packId = packId,
    subject = subject,
    startedAtLocal = startedAtLocal,
    finishedAtLocal = finishedAtLocal,
    durationMs = durationMs,
    status = status,
    scoreRaw = scoreRaw,
    scoreValidated = scoreValidated,
    origin = origin,
    syncState = syncState,
)

fun ExamAttempt.toEntity() = ExamAttemptEntity(
    attemptId = attemptId,
    uid = uid,
    packId = packId,
    subject = subject,
    startedAtLocal = startedAtLocal,
    finishedAtLocal = finishedAtLocal,
    durationMs = durationMs,
    status = status,
    scoreRaw = scoreRaw,
    scoreValidated = scoreValidated,
    origin = origin,
    syncState = syncState,
)

fun ExamAnswerEntity.toDomain() = ExamAnswer(
    attemptId = attemptId,
    questionId = questionId,
    selectedOptionId = selectedOptionId,
    isCorrect = isCorrect,
    timeSpentMs = timeSpentMs,
)

fun ExamAnswer.toEntity() = ExamAnswerEntity(
    attemptId = attemptId,
    questionId = questionId,
    selectedOptionId = selectedOptionId,
    isCorrect = isCorrect,
    timeSpentMs = timeSpentMs,
)
