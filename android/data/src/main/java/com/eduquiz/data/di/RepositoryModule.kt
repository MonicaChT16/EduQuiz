package com.eduquiz.data.di

import com.eduquiz.data.repository.ExamRepositoryImpl
import com.eduquiz.data.repository.PackRepositoryImpl
import com.eduquiz.data.repository.ProfileRepositoryImpl
import com.eduquiz.data.repository.StoreRepositoryImpl
import com.eduquiz.data.repository.RankingRepositoryImpl
import com.eduquiz.data.repository.SyncRepositoryImpl
import com.eduquiz.domain.exam.ExamRepository
import com.eduquiz.domain.pack.PackRepository
import com.eduquiz.domain.profile.ProfileRepository
import com.eduquiz.domain.ranking.RankingRepository
import com.eduquiz.domain.store.StoreRepository
import com.eduquiz.domain.sync.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPackRepository(impl: PackRepositoryImpl): PackRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository

    @Binds
    @Singleton
    abstract fun bindRankingRepository(impl: RankingRepositoryImpl): RankingRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository
}
