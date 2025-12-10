package com.eduquiz.data.di

import com.eduquiz.data.remote.FirestorePackRemoteDataSource
import com.eduquiz.data.remote.PackRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        android.util.Log.d("RemoteModule", "Providing Firestore instance: ${firestore.app.name}")
        return firestore
    }

    @Provides
    @Singleton
    fun providePackRemoteDataSource(
        firestore: FirebaseFirestore
    ): PackRemoteDataSource = FirestorePackRemoteDataSource(firestore)
}
