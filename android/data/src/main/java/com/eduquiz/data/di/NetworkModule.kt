package com.eduquiz.data.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.eduquiz.data.network.NetworkRepositoryImpl
import com.eduquiz.domain.network.NetworkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindNetworkRepository(
        impl: NetworkRepositoryImpl
    ): NetworkRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideNetworkRepositoryImpl(
            @ApplicationContext context: Context
        ): NetworkRepositoryImpl {
            return NetworkRepositoryImpl(context)
        }
    }
}

