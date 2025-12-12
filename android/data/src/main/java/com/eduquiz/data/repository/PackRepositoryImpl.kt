package com.eduquiz.data.repository

import androidx.room.withTransaction
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.PackDao
import com.eduquiz.data.remote.OptionRemote
import com.eduquiz.data.remote.PackMetaRemote
import com.eduquiz.data.remote.PackRemoteDataSource
import com.eduquiz.data.remote.QuestionRemote
import com.eduquiz.data.remote.TextRemote
import com.eduquiz.domain.pack.Option
import com.eduquiz.domain.pack.Pack
import com.eduquiz.domain.pack.PackMeta
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
    private val database: AppDatabase,
    private val remoteDataSource: PackRemoteDataSource,
) : PackRepository {

    override suspend fun fetchCurrentPackMeta(): PackMeta? {
        return try {
            remoteDataSource.fetchCurrentPackMeta()?.toPackMeta()
        } catch (e: Exception) {
            android.util.Log.e("PackRepositoryImpl", "Error fetching pack meta", e)
            throw e // Propagar el error para que se muestre en la UI
        }
    }

    override suspend fun downloadPack(packId: String): Pack {
        val existing = packDao.findById(packId)?.toDomain()
        if (existing != null) {
            database.withTransaction {
                packDao.markAsActive(packId)
                packDao.updateStatusForOthers(
                    packId = packId,
                    currentStatus = PackStatus.DOWNLOADED,
                    newStatus = PackStatus.ARCHIVED
                )
            }
            return existing.copy(status = PackStatus.ACTIVE)
        }

        val bundle = remoteDataSource.fetchPack(packId)
            ?: error("El pack $packId no existe en Firestore.")

        val now = System.currentTimeMillis()
        val pack = bundle.meta.toPack(downloadedAt = now)
        val texts = bundle.texts.map { it.toDomain() }
        val questions = bundle.questions.map { it.toDomain() }
        val options = bundle.questions.flatMap { question ->
            question.options.map { it.toDomain(question.questionId) }
        }

        // Validar que tenemos datos antes de guardar
        if (texts.isEmpty()) {
            error("El pack $packId no tiene textos asociados. Se esperaban ${bundle.meta.textIds.size} textos pero se encontraron ${bundle.texts.size}.")
        }
        if (questions.isEmpty()) {
            error("El pack $packId no tiene preguntas asociadas. Se esperaban ${bundle.meta.questionIds.size} preguntas pero se encontraron ${bundle.questions.size}. IDs esperados: ${bundle.meta.questionIds.joinToString()}")
        }
        if (options.isEmpty()) {
            val questionsWithOptions = bundle.questions.count { it.options.isNotEmpty() }
            error("El pack $packId no tiene opciones asociadas a las preguntas. De ${bundle.questions.size} preguntas, solo ${questionsWithOptions} tienen opciones.")
        }

        database.withTransaction {
            packDao.insert(pack.toEntity())
            contentDao.insertTexts(texts.map { it.toEntity() })
            contentDao.insertQuestions(questions.map { it.toEntity() })
            contentDao.insertOptions(options.map { it.toEntity() })
            packDao.markAsActive(pack.packId)
            packDao.updateStatusForOthers(
                packId = pack.packId,
                currentStatus = PackStatus.DOWNLOADED,
                newStatus = PackStatus.ARCHIVED
            )
        }

        return pack.copy(status = PackStatus.ACTIVE)
    }

    override suspend fun getPackById(packId: String): Pack? = packDao.findById(packId)?.toDomain()

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
    
    override suspend fun getActivePack(): Pack? =
        packDao.findByStatus(PackStatus.ACTIVE)?.toDomain()

    override suspend fun getTextsForPack(packId: String): List<TextContent> =
        contentDao.getTextsByPack(packId).map { it.toDomain() }

    override suspend fun getQuestionsForText(textId: String): List<Question> =
        contentDao.getQuestionsByText(textId).map { it.toDomain() }

    override suspend fun getQuestionsForPack(packId: String): List<Question> =
        contentDao.getQuestionsByPack(packId).map { it.toDomain() }

    override suspend fun getQuestionsForPackBySubject(packId: String, subject: String): List<Question> =
        contentDao.getQuestionsByPackAndSubject(packId, subject).map { it.toDomain() }

    override suspend fun getOptionsForQuestion(questionId: String): List<Option> =
        contentDao.getOptionsByQuestion(questionId).map { it.toDomain() }
}

private fun PackMetaRemote.toPack(downloadedAt: Long): Pack {
    return Pack(
        packId = packId,
        weekLabel = weekLabel,
        status = PackStatus.DOWNLOADED,
        publishedAt = publishedAt,
        downloadedAt = downloadedAt
    )
}

private fun PackMetaRemote.toPackMeta(): PackMeta {
    return PackMeta(
        packId = packId,
        weekLabel = weekLabel,
        status = status,
        publishedAt = publishedAt,
        textIds = textIds,
        questionIds = questionIds
    )
}

private fun TextRemote.toDomain(): TextContent {
    return TextContent(
        textId = textId,
        packId = packId,
        title = title,
        body = body,
        subject = subject
    )
}

private fun QuestionRemote.toDomain(): Question {
    return Question(
        questionId = questionId,
        packId = packId,
        textId = textId,
        prompt = prompt,
        correctOptionId = correctOptionId,
        difficulty = difficulty,
        explanationText = explanationText,
        explanationStatus = explanationStatus
    )
}

private fun OptionRemote.toDomain(questionId: String): Option {
    return Option(
        questionId = questionId,
        optionId = optionId,
        text = text
    )
}
