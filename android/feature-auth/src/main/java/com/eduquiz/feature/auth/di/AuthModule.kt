package com.eduquiz.feature.auth.di

import com.eduquiz.feature.auth.data.AuthRepository
import com.eduquiz.feature.auth.data.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository = firebaseAuthRepository
}
