package com.eduquiz.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkingTest {

    @Test
    fun `chunks list respecting firestore limit`() {
        val values = (1..23).map { "id-$it" }

        val chunks = chunkForWhereIn(values)

        assertEquals(listOf(10, 10, 3), chunks.map { it.size })
        assertEquals(values.first(), chunks.first().first())
        assertEquals(values.last(), chunks.last().last())
    }

    @Test
    fun `returns empty when list is empty`() {
        val chunks = chunkForWhereIn(emptyList<String>())
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `accepts custom chunk size`() {
        val values = (1..5).toList()
        val chunks = chunkForWhereIn(values, chunkSize = 2)

        assertEquals(3, chunks.size)
        assertEquals(listOf(2, 2, 1), chunks.map { it.size })
    }
}
