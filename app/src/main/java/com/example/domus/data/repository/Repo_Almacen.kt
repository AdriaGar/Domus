package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.AlmacenDao
import com.example.domus.app.Almacen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class Repo_Almacen(private val almacenDao: AlmacenDao) {

    private val TAG = "DomusDebug"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var snapshotListener: ListenerRegistration? = null

    val allAlmacenes: Flow<List<Almacen>> = almacenDao.getAll()

    private fun getCollection(familiaId: String?): CollectionReference {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return if (familiaId != null) {
            firestore.collection("families").document(familiaId).collection("almacenes")
        } else {
            firestore.collection("users").document(userId).collection("almacenes")
        }
    }

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        
        val collection = try { getCollection(familiaId) } catch (e: Exception) { return }

        snapshotListener = collection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de Almacenes", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val almacenes = snapshots.toObjects(Almacen::class.java)
                repositoryScope.launch {
                    try {
                        almacenDao.deleteAll()
                        almacenDao.insertAll(almacenes)
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error al actualizar Room Almacenes", dbError)
                    }
                }
            }
        }
    }

    suspend fun addAlmacen(almacen: Almacen, familiaId: String?) {
        val user = auth.currentUser ?: return
        try {
            val collection = getCollection(familiaId)
            val docRef = collection.document()
            val finalAlmacen = almacen.copy(
                id = docRef.id,
                usuarioId = user.uid,
                familiaId = familiaId
            )
            almacenDao.insertAll(listOf(finalAlmacen))
            docRef.set(finalAlmacen)
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir almacén", e)
        }
    }

    suspend fun deleteAlmacen(almacen: Almacen) {
        if (almacen.id.isNotEmpty()) {
            try {
                almacenDao.delete(almacen)
                getCollection(almacen.familiaId).document(almacen.id).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar almacén", e)
            }
        }
    }
}
