package com.eduquiz.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para manejar la subida de imágenes a Firebase Storage.
 */
@Singleton
class ImageStorageService @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) {
    
    /**
     * Sube una imagen de perfil y retorna la URL de descarga.
     * @param uid ID del usuario
     * @param imageUri URI de la imagen a subir
     * @return URL de descarga de la imagen subida
     */
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String {
        val storageRef: StorageReference = firebaseStorage.reference
        val profileImagesRef: StorageReference = storageRef.child("profile_images/$uid.jpg")
        
        android.util.Log.d("ImageStorageService", "Iniciando subida de imagen para usuario: $uid")
        android.util.Log.d("ImageStorageService", "Ruta de Storage: profile_images/$uid.jpg")
        
        try {
            // Subir la imagen y esperar a que termine
            val uploadTask = profileImagesRef.putFile(imageUri)
            val snapshot = uploadTask.await()
            
            android.util.Log.d("ImageStorageService", "Imagen subida exitosamente. Bytes transferidos: ${snapshot.bytesTransferred}")
            android.util.Log.d("ImageStorageService", "Obteniendo URL de descarga...")
            
            // Esperar un momento para asegurar que el objeto esté disponible
            delay(500)
            
            // Obtener la URL de descarga usando el reference original
            val downloadUrl = try {
                profileImagesRef.downloadUrl.await()
            } catch (e: Exception) {
                android.util.Log.e("ImageStorageService", "Error obteniendo URL (intento 1): ${e.message}", e)
                android.util.Log.e("ImageStorageService", "Tipo de error: ${e.javaClass.simpleName}")
                // Si falla, intentar una vez más después de otro delay
                delay(1000)
                try {
                    profileImagesRef.downloadUrl.await()
                } catch (e2: Exception) {
                    android.util.Log.e("ImageStorageService", "Error obteniendo URL (intento 2): ${e2.message}", e2)
                    throw IllegalStateException(
                        "No se pudo obtener la URL de descarga. " +
                        "Verifica que las reglas de Storage permitan lectura. " +
                        "Error: ${e2.message}",
                        e2
                    )
                }
            }
            
            android.util.Log.d("ImageStorageService", "URL obtenida exitosamente: ${downloadUrl.toString()}")
            return downloadUrl.toString()
        } catch (e: Exception) {
            android.util.Log.e("ImageStorageService", "Error durante la subida: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Elimina la imagen de perfil del usuario.
     */
    suspend fun deleteProfileImage(uid: String) {
        val storageRef: StorageReference = firebaseStorage.reference
        val profileImagesRef: StorageReference = storageRef.child("profile_images/$uid.jpg")
        profileImagesRef.delete().await()
    }
}


