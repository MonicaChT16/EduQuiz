package com.eduquiz.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EduQuizApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase explícitamente
        try {
            val firebaseApp = FirebaseApp.initializeApp(this)
            firebaseApp?.let {
                Log.d("EduQuizApp", "Firebase initialized: ${it.name}")
            } ?: Log.d("EduQuizApp", "Firebase initialized: [DEFAULT]")
            
            // Verificar que Firestore esté disponible
            val firestore = FirebaseFirestore.getInstance()
            Log.d("EduQuizApp", "Firestore instance created successfully")
            Log.d("EduQuizApp", "Firestore app: ${firestore.app.name}")
        } catch (e: Exception) {
            Log.e("EduQuizApp", "Error initializing Firebase", e)
        }
        
        // WorkManager will use getWorkManagerConfiguration() provided below.
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

