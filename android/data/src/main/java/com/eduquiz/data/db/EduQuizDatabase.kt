package com.eduquiz.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "sample_entities")
data class SampleEntity(
    @PrimaryKey val id: String,
    val title: String,
)

@Dao
interface SampleDao {
    @Query("SELECT * FROM sample_entities")
    suspend fun getAll(): List<SampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entities: SampleEntity)
}

@Database(entities = [SampleEntity::class], version = 1)
abstract class EduQuizDatabase : RoomDatabase() {
    abstract fun sampleDao(): SampleDao
}
