package com.eduquiz.data.remote

import com.eduquiz.domain.pack.ExplanationStatus
import com.eduquiz.domain.pack.Subject
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
        return try {
            android.util.Log.d("PackRemoteDataSource", "=== INICIANDO CONSULTA A FIRESTORE ===")
            android.util.Log.d("PackRemoteDataSource", "Firestore app: ${firestore.app.name}")
            android.util.Log.d("PackRemoteDataSource", "Collection: $PACKS_COLLECTION")
            android.util.Log.d("PackRemoteDataSource", "Status filter: $STATUS_PUBLISHED")
            
            // Obtener todos los packs publicados y ordenar en memoria
            // Esto evita la necesidad de un índice compuesto en Firestore
            android.util.Log.d("PackRemoteDataSource", "Ejecutando consulta...")
            val snapshots = firestore.collection(PACKS_COLLECTION)
                .whereEqualTo("status", STATUS_PUBLISHED)
                .get()
                .await()
                .documents

            android.util.Log.d("PackRemoteDataSource", "✅ Consulta completada. Found ${snapshots.size} published packs")

            // Ordenar por publishedAt descendente y tomar el más reciente
            val snapshot = snapshots
                .sortedByDescending { it.getLong("publishedAt") ?: 0L }
                .firstOrNull()

            if (snapshot == null) {
                android.util.Log.w("PackRemoteDataSource", "No published pack found")
                return null
            }

            val meta = snapshot.toPackMeta()
            android.util.Log.d("PackRemoteDataSource", "Successfully fetched pack meta: ${meta?.packId}")
            meta
        } catch (e: Exception) {
            android.util.Log.e("PackRemoteDataSource", "Error fetching current pack meta", e)
            android.util.Log.e("PackRemoteDataSource", "Error message: ${e.message}")
            android.util.Log.e("PackRemoteDataSource", "Error cause: ${e.cause?.message}")
            throw e
        }
    }

    override suspend fun fetchPack(packId: String): PackBundleRemote? {
        return try {
            android.util.Log.d("PackRemoteDataSource", "Fetching pack $packId from Firestore...")
            val packSnapshot = firestore.collection(PACKS_COLLECTION)
                .document(packId)
                .get()
                .await()

            if (!packSnapshot.exists()) {
                android.util.Log.w("PackRemoteDataSource", "Pack $packId does not exist in Firestore")
                return null
            }

            val meta = packSnapshot.toPackMeta() ?: run {
                android.util.Log.w("PackRemoteDataSource", "Failed to parse pack meta for $packId")
                return null
            }
            
            android.util.Log.d("PackRemoteDataSource", "Fetching ${meta.textIds.size} texts and ${meta.questionIds.size} questions...")
            val texts = fetchTexts(meta)
            val questions = fetchQuestions(meta)

            android.util.Log.d("PackRemoteDataSource", "Successfully fetched pack: ${texts.size} texts, ${questions.size} questions")
            
            PackBundleRemote(
                meta = meta,
                texts = texts,
                questions = questions
            )
        } catch (e: Exception) {
            android.util.Log.e("PackRemoteDataSource", "Error fetching pack $packId", e)
            android.util.Log.e("PackRemoteDataSource", "Error message: ${e.message}")
            android.util.Log.e("PackRemoteDataSource", "Error cause: ${e.cause?.message}")
            throw e
        }
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
        // Normalizar el subject: convertir valores antiguos a los nuevos
        val rawSubject = getString("subject") ?: "GENERAL"
        val subject = when (rawSubject.uppercase()) {
            "LECTURA", "LECTURA_COMPRENSION", "COMPRENSION" -> Subject.COMPRENSION_LECTORA
            "MATEMATICA", "MATEMATICAS", "MATH" -> Subject.MATEMATICA
            "CIENCIAS", "CIENCIA", "SCIENCE" -> Subject.CIENCIAS
            else -> rawSubject.uppercase()
        }
        val packId = getString("packId") ?: defaultPackId
        android.util.Log.d("PackRemoteDataSource", "Text $id: subject=$rawSubject -> normalized=$subject")
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
