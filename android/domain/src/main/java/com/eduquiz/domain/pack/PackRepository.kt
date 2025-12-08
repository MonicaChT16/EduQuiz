package com.eduquiz.domain.pack

import kotlinx.coroutines.flow.Flow

interface PackRepository {
    suspend fun fetchCurrentPackMeta(): PackMeta?
    suspend fun downloadPack(packId: String): Pack
    suspend fun getPackById(packId: String): Pack?

    suspend fun insertPack(pack: Pack)
    suspend fun insertTexts(texts: List<TextContent>)
    suspend fun insertQuestions(questions: List<Question>)
    suspend fun insertOptions(options: List<Option>)

    suspend fun setActivePack(packId: String)
    suspend fun updatePackStatus(packId: String, status: String)
    fun observeActivePack(): Flow<Pack?>

    suspend fun getTextsForPack(packId: String): List<TextContent>
    suspend fun getQuestionsForText(textId: String): List<Question>
    suspend fun getQuestionsForPack(packId: String): List<Question>
    suspend fun getOptionsForQuestion(questionId: String): List<Option>
}
