package com.eduquiz.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eduquiz.domain.pack.PackRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que verifica y descarga automáticamente nuevos packs cuando están disponibles.
 * 
 * Requisitos:
 * - Network connected constraint
 * - Verifica si hay un pack nuevo disponible
 * - Descarga automáticamente sin avisar al usuario
 * - Se ejecuta periódicamente en segundo plano
 */
@HiltWorker
class PackUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val packRepository: PackRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PackUpdateWorker"
        const val WORK_NAME = "com.eduquiz.data.sync.PackUpdateWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting pack update check")

            // 1. Obtener el pack activo actual (si existe)
            val activePack = packRepository.getActivePack()
            val currentPackId = activePack?.packId
            Log.d(TAG, "Current active pack: ${currentPackId ?: "none"}")

            // 2. Verificar si hay un pack nuevo disponible en Firestore
            val availablePackMeta = packRepository.fetchCurrentPackMeta()

            if (availablePackMeta == null) {
                Log.d(TAG, "No pack available in Firestore")
                return Result.success()
            }

            // 3. Comparar: si el pack disponible es diferente al activo, o no hay pack activo, descargarlo
            if (currentPackId == null || currentPackId != availablePackMeta.packId) {
                if (currentPackId == null) {
                    Log.d(TAG, "No active pack found, downloading available pack: ${availablePackMeta.packId}")
                } else {
                    Log.d(TAG, "New pack available: ${availablePackMeta.packId} (current: $currentPackId)")
                }
                
                try {
                    // Descargar el nuevo pack automáticamente
                    val downloadedPack = packRepository.downloadPack(availablePackMeta.packId)
                    Log.d(TAG, "Successfully downloaded new pack: ${downloadedPack.packId}")
                    
                    // Marcar como activo
                    packRepository.setActivePack(downloadedPack.packId)
                    Log.d(TAG, "New pack activated: ${downloadedPack.packId}")
                    
                    return Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading pack ${availablePackMeta.packId}", e)
                    // No retry inmediatamente, esperar al próximo ciclo
                    return Result.success() // Success para no bloquear otros workers
                }
            } else {
                Log.d(TAG, "Current pack is up to date: $currentPackId")
                return Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in pack update work", e)
            // No retry para evitar consumo excesivo de batería
            // El próximo ciclo periódico lo intentará de nuevo
            return Result.success()
        }
    }
}

