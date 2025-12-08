package com.eduquiz.data.repository

import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.PackDao
import com.eduquiz.domain.pack.Option
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.pack.PackStatus
import com.eduquiz.domain.pack.Question
import com.eduquiz.domain.pack.TextContent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PackRepositoryImpl @Inject constructor(
    private val packDao: PackDao,
    private val contentDao: ContentDao,
) : PackRepository {

    override suspend fun insertPack(pack: Pack) {
        packDao.insert(pack.toEntity())
    }

    override suspend fun insertTexts(texts: List<TextContent>) {
        if (texts.isNotEmpty()) {
            contentDao.insertTexts(texts.map { it.toEntity() })
        }
    }

    override suspend fun insertQuestions(questions: List<Question>) {
        if (questions.isNotEmpty()) {
            contentDao.insertQuestions(questions.map { it.toEntity() })
        }
    }

    override suspend fun insertOptions(options: List<Option>) {
        if (options.isNotEmpty()) {
            contentDao.insertOptions(options.map { it.toEntity() })
        }
    }

    override suspend fun setActivePack(packId: String) {
        packDao.markAsActive(packId)
    }

    override suspend fun updatePackStatus(packId: String, status: String) {
        packDao.updateStatus(packId, status)
    }

    override fun observeActivePack(): Flow<Pack?> =
        packDao.observeByStatus(PackStatus.ACTIVE).map { it?.toDomain() }

    override suspend fun getTextsForPack(packId: String): List<TextContent> =
        contentDao.getTextsByPack(packId).map { it.toDomain() }

    override suspend fun getQuestionsForText(textId: String): List<Question> =
        contentDao.getQuestionsByText(textId).map { it.toDomain() }

    override suspend fun getQuestionsForPack(packId: String): List<Question> =
        contentDao.getQuestionsByPack(packId).map { it.toDomain() }

    override suspend fun getOptionsForQuestion(questionId: String): List<Option> =
        contentDao.getOptionsByQuestion(questionId).map { it.toDomain() }
}
