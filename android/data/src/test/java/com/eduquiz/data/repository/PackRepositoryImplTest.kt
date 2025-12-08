package com.eduquiz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.remote.OptionRemote
import com.eduquiz.data.remote.PackBundleRemote
import com.eduquiz.data.remote.PackMetaRemote
import com.eduquiz.data.remote.PackRemoteDataSource
import com.eduquiz.data.remote.QuestionRemote
import com.eduquiz.data.remote.TextRemote
import com.eduquiz.domain.pack.ExplanationStatus
import com.eduquiz.domain.pack.PackStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class PackRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PackRepositoryImpl
    private lateinit var remoteDataSource: FakePackRemoteDataSource

    @BeforeTest
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remoteDataSource = FakePackRemoteDataSource(
            bundles = mapOf(
                "pack-1" to buildBundle("pack-1"),
                "pack-2" to buildBundle("pack-2")
            )
        )
        repository = PackRepositoryImpl(
            packDao = db.packDao(),
            contentDao = db.contentDao(),
            database = db,
            remoteDataSource = remoteDataSource
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun `downloadPack stores content and marks active`() = runTest {
        repository.downloadPack("pack-1")

        val active = db.packDao().observeByStatus(PackStatus.ACTIVE).first()
        assertNotNull(active)
        assertEquals("pack-1", active.packId)
        assertEquals(PackStatus.ACTIVE, active.status)

        val texts = db.contentDao().getTextsByPack("pack-1")
        assertEquals(1, texts.size)

        val questions = db.contentDao().getQuestionsByPack("pack-1")
        assertEquals(1, questions.size)

        val options = db.contentDao().getOptionsByQuestion("question-pack-1")
        assertEquals(2, options.size)
    }

    @Test
    fun `downloading new pack archives previous one`() = runTest {
        repository.downloadPack("pack-1")
        repository.downloadPack("pack-2")

        val active = db.packDao().observeByStatus(PackStatus.ACTIVE).first()
        assertEquals("pack-2", active?.packId)

        val archived = db.packDao().findById("pack-1")
        assertEquals(PackStatus.ARCHIVED, archived?.status)
    }

    private fun buildBundle(packId: String): PackBundleRemote {
        val textId = "text-$packId"
        val questionId = "question-$packId"
        return PackBundleRemote(
            meta = PackMetaRemote(
                packId = packId,
                weekLabel = "Semana $packId",
                status = "PUBLISHED",
                publishedAt = 1L,
                textIds = listOf(textId),
                questionIds = listOf(questionId)
            ),
            texts = listOf(
                TextRemote(
                    textId = textId,
                    packId = packId,
                    title = "Titulo $packId",
                    body = "Contenido $packId",
                    subject = "LECTURA"
                )
            ),
            questions = listOf(
                QuestionRemote(
                    questionId = questionId,
                    packId = packId,
                    textId = textId,
                    prompt = "Pregunta $packId",
                    correctOptionId = "option-1",
                    difficulty = 1,
                    explanationText = null,
                    explanationStatus = ExplanationStatus.NONE,
                    options = listOf(
                        OptionRemote(optionId = "option-1", text = "A"),
                        OptionRemote(optionId = "option-2", text = "B"),
                    )
                )
            )
        )
    }
}

private class FakePackRemoteDataSource(
    private val bundles: Map<String, PackBundleRemote>
) : PackRemoteDataSource {
    override suspend fun fetchCurrentPackMeta(): PackMetaRemote? = bundles.values.firstOrNull()?.meta
    override suspend fun fetchPack(packId: String): PackBundleRemote? = bundles[packId]
}
