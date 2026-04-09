package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.Entity.Entity_Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class Repo_Usuario {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun sendNewUser(usuario: Entity_Usuario) : Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("No hay sesión de usuario activa"))
            
            val userData = mapOf(
                "nombre" to usuario.nombre,
                "apellidos" to usuario.apellidos,
                "email" to usuario.email,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            usersCollection.document(uid).set(userData, SetOptions.merge()).await()
            
            Log.d("Repo_Usuario", "Usuario guardado en Firestore correctamente")
            Result.success(true)
        } catch (e: Exception) {
            val errorMsg = "Error al guardar usuario en Firestore: ${e.message}"
            Log.e("Repo_Usuario", errorMsg, e)
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun updateFamilyId(familyId: String?) {
        try {
            val uid = auth.currentUser?.uid ?: return
            val data = if (familyId == null) {
                mapOf("familyId" to com.google.firebase.firestore.FieldValue.delete())
            } else {
                mapOf("familyId" to familyId)
            }
            usersCollection.document(uid).update(data).await()
        } catch (e: Exception) {
            Log.e("Repo_Usuario", "Error al actualizar familyId", e)
        }
    }
}
