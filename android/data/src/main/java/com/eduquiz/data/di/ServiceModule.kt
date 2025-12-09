package com.eduquiz.data.di

import com.eduquiz.data.repository.AchievementEngineImpl
import com.eduquiz.data.repository.StreakServiceImpl
import com.eduquiz.domain.achievements.AchievementEngine
import com.eduquiz.domain.streak.StreakService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindStreakService(impl: StreakServiceImpl): StreakService

    @Binds
    @Singleton
    abstract fun bindAchievementEngine(impl: AchievementEngineImpl): AchievementEngine
}


