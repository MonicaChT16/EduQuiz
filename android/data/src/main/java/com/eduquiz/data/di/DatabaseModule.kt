package com.eduquiz.data.di

import android.content.Context
import androidx.room.Room
import com.eduquiz.data.db.AchievementsDao
import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.db.ContentDao
import com.eduquiz.data.db.ExamDao
import com.eduquiz.data.db.PackDao
import com.eduquiz.data.db.ProfileDao
import com.eduquiz.data.db.StoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun providePackDao(db: AppDatabase): PackDao = db.packDao()

    @Provides
    fun provideContentDao(db: AppDatabase): ContentDao = db.contentDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideStoreDao(db: AppDatabase): StoreDao = db.storeDao()

    @Provides
    fun provideAchievementsDao(db: AppDatabase): AchievementsDao = db.achievementsDao()

    @Provides
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
}
