package com.eduquiz.data.di

import com.eduquiz.data.db.AppDatabase
import com.eduquiz.data.remote.FirestoreSyncService
import com.eduquiz.data.sync.SyncWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideSyncWorkerFactory(
        databaseProvider: Provider<AppDatabase>,
        syncServiceProvider: Provider<FirestoreSyncService>
    ): SyncWorker.Factory {
        return SyncWorker.Factory(
            databaseProvider = databaseProvider,
            syncServiceProvider = syncServiceProvider
        )
    }
}

