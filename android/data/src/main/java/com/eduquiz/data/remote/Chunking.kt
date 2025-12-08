package com.eduquiz.data.remote

internal const val FIRESTORE_WHERE_IN_LIMIT = 10

/**
 * Splits a list into chunks that respect Firestore whereIn limit (10 values max).
 */
fun <T> chunkForWhereIn(values: List<T>, chunkSize: Int = FIRESTORE_WHERE_IN_LIMIT): List<List<T>> {
    if (values.isEmpty()) return emptyList()
    require(chunkSize > 0) { "chunkSize must be > 0" }
    return values.chunked(chunkSize)
}
