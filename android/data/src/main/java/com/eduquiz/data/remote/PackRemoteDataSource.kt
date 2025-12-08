package com.eduquiz.data.remote

import com.eduquiz.domain.pack.ExplanationStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

data class PackMetaRemote(
    val packId: String,
    val weekLabel: String,
    val status: String,
    val publishedAt: Long,
    val textIds: List<String>,
    val questionIds: List<String>,
)

data class TextRemote(
    val textId: String,
    val packId: String,
    val title: String,
    val body: String,
    val subject: String,
)

data class OptionRemote(
    val optionId: String,
    val text: String,
)

data class QuestionRemote(
    val questionId: String,
    val packId: String,
    val textId: String,
    val prompt: String,
    val correctOptionId: String,
    val difficulty: Int,
    val explanationText: String?,
    val explanationStatus: String,
    val options: List<OptionRemote> = emptyList(),
)

data class PackBundleRemote(
    val meta: PackMetaRemote,
    val texts: List<TextRemote>,
    val questions: List<QuestionRemote>,
)

interface PackRemoteDataSource {
    suspend fun fetchCurrentPackMeta(): PackMetaRemote?
    suspend fun fetchPack(packId: String): PackBundleRemote?
}

class FirestorePackRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PackRemoteDataSource {

    override suspend fun fetchCurrentPackMeta(): PackMetaRemote? {
        val snapshot = firestore.collection(PACKS_COLLECTION)
            .whereEqualTo("status", STATUS_PUBLISHED)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        return snapshot?.toPackMeta()
    }

    override suspend fun fetchPack(packId: String): PackBundleRemote? {
        val packSnapshot = firestore.collection(PACKS_COLLECTION)
            .document(packId)
            .get()
            .await()

        val meta = packSnapshot.toPackMeta() ?: return null
        val texts = fetchTexts(meta)
        val questions = fetchQuestions(meta)

        return PackBundleRemote(
            meta = meta,
            texts = texts,
            questions = questions
        )
    }

    private suspend fun fetchTexts(meta: PackMetaRemote): List<TextRemote> {
        return fetchByIds(
            ids = meta.textIds,
            collection = TEXTS_COLLECTION
        ) { snapshot ->
            snapshot.toTextRemote(meta.packId)
        }
    }

    private suspend fun fetchQuestions(meta: PackMetaRemote): List<QuestionRemote> {
        val questions = fetchByIds(
            ids = meta.questionIds,
            collection = QUESTIONS_COLLECTION
        ) { snapshot ->
            snapshot.toQuestionRemote(meta.packId)
        }

        return questions.map { question ->
            if (question.options.isNotEmpty()) {
                question
            } else {
                val options = fetchOptions(question.questionId)
                question.copy(options = options)
            }
        }
    }

    private suspend fun fetchOptions(questionId: String): List<OptionRemote> {
        val subCollection = firestore.collection(QUESTIONS_COLLECTION)
            .document(questionId)
            .collection(OPTIONS_COLLECTION)
            .get()
            .await()
            .documents
            .mapNotNull { it.toOptionRemote() }

        if (subCollection.isNotEmpty()) {
            return subCollection
        }

        val topLevel = firestore.collection(OPTIONS_COLLECTION)
            .whereEqualTo("questionId", questionId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toOptionRemote(defaultQuestionId = questionId) }

        return topLevel
    }

    private suspend fun <T : Any> fetchByIds(
        ids: List<String>,
        collection: String,
        mapper: (DocumentSnapshot) -> T?
    ): List<T> {
        if (ids.isEmpty()) return emptyList()

        val results = mutableListOf<T>()
        for (chunk in chunkForWhereIn(ids)) {
            val snapshots = firestore.collection(collection)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents

            snapshots.mapNotNullTo(results, mapper)
        }
        return results
    }

    private fun chunkForWhereIn(ids: List<String>): List<List<String>> {
        // Firestore whereIn supports max 10 items
        return ids.chunked(10)
    }

    private fun DocumentSnapshot.toPackMeta(): PackMetaRemote? {
        val weekLabel = getString("weekLabel") ?: return null
        val status = getString("status") ?: STATUS_PUBLISHED
        val publishedAt = getLong("publishedAt") ?: 0L
        val textIds = (get("textIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val questionIds =
            (get("questionIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        return PackMetaRemote(
            packId = id,
            weekLabel = weekLabel,
            status = status,
            publishedAt = publishedAt,
            textIds = textIds,
            questionIds = questionIds
        )
    }

    private fun DocumentSnapshot.toTextRemote(defaultPackId: String): TextRemote? {
        val title = getString("title") ?: return null
        val body = getString("body") ?: return null
        val subject = getString("subject") ?: "GENERAL"
        val packId = getString("packId") ?: defaultPackId
        return TextRemote(
            textId = id,
            packId = packId,
            title = title,
            body = body,
            subject = subject
        )
    }

    private fun DocumentSnapshot.toQuestionRemote(defaultPackId: String): QuestionRemote? {
        val prompt = getString("prompt") ?: return null
        val correctOptionId = getString("correctOptionId") ?: return null
        val difficulty = (getLong("difficulty") ?: 1L).toInt()
        val explanationStatus = getString("explanationStatus") ?: ExplanationStatus.NONE
        val explanationText = getString("explanationText")
        val packId = getString("packId") ?: defaultPackId
        val textId = getString("textId") ?: return null
        val options = parseEmbeddedOptions()

        return QuestionRemote(
            questionId = id,
            packId = packId,
            textId = textId,
            prompt = prompt,
            correctOptionId = correctOptionId,
            difficulty = difficulty,
            explanationText = explanationText,
            explanationStatus = explanationStatus,
            options = options
        )
    }

    private fun DocumentSnapshot.toOptionRemote(defaultQuestionId: String? = null): OptionRemote? {
        val optionId = id.takeIf { it.isNotBlank() }
            ?: (getString("optionId") ?: return null)
        val text = getString("text") ?: return null
        val questionId = getString("questionId") ?: defaultQuestionId
        if (questionId == null) return null
        return OptionRemote(
            optionId = optionId,
            text = text
        )
    }

    private fun DocumentSnapshot.parseEmbeddedOptions(): List<OptionRemote> {
        val rawOptions = get("options") as? List<*> ?: return emptyList()
        return rawOptions.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            val optionId = item["optionId"] as? String ?: return@mapNotNull null
            val text = item["text"] as? String ?: return@mapNotNull null
            OptionRemote(optionId = optionId, text = text)
        }
    }

    private companion object {
        const val PACKS_COLLECTION = "packs"
        const val TEXTS_COLLECTION = "texts"
        const val QUESTIONS_COLLECTION = "questions"
        const val OPTIONS_COLLECTION = "options"
        const val STATUS_PUBLISHED = "PUBLISHED"
    }
}
