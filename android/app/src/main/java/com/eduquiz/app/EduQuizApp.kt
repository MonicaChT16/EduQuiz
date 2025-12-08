package com.eduquiz.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.eduquiz.data.sync.SyncWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EduQuizApp : Application() {

    @Inject
    lateinit var workerFactory: SyncWorker.Factory

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Inicializar WorkManager con el WorkerFactory personalizado
        // Solo inicializar si no está ya inicializado
        try {
            val config = Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
            WorkManager.initialize(this, config)
        } catch (e: IllegalStateException) {
            // WorkManager ya está inicializado, ignorar
            android.util.Log.d("EduQuizApp", "WorkManager already initialized")
        }
    }
}
