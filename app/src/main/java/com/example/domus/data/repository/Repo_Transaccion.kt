package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.TransaccionDao
import com.example.domus.data.Entity.Transaccion
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
import kotlinx.coroutines.tasks.await

class Repo_Transaccion(private val transaccionDao: TransaccionDao) {

    private val TAG = "DomusDebug"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var snapshotListener: ListenerRegistration? = null

    val allTransacciones: Flow<List<Transaccion>> = transaccionDao.getAll()

    private fun getCollection(familiaId: String?): CollectionReference {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return if (!familiaId.isNullOrEmpty()) {
            firestore.collection("families").document(familiaId).collection("transacciones")
        } else {
            firestore.collection("users").document(userId).collection("transacciones")
        }
    }

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        
        val collection = try { getCollection(familiaId) } catch (e: Exception) { return }

        snapshotListener = collection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de Firestore.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val transacciones = snapshots.toObjects(Transaccion::class.java)
                repositoryScope.launch {
                    try {
                        transaccionDao.deleteAll()
                        transaccionDao.insertAll(transacciones)
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error al actualizar Room", dbError)
                    }
                }
            }
        }
    }

    suspend fun addTransaccion(transaccion: Transaccion, familiaId: String?) {
        val user = auth.currentUser ?: return
        try {
            val collection = getCollection(familiaId)
            val docRef = collection.document()
            val finalTransaccion = transaccion.copy(
                id = docRef.id,
                usuarioId = user.uid,
                familiaId = familiaId
            )
            transaccionDao.insertAll(listOf(finalTransaccion))
            docRef.set(finalTransaccion)
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir transacción", e)
        }
    }

    suspend fun updateTransaccion(transaccion: Transaccion) {
        if (transaccion.id.isNotEmpty()) {
            try {
                transaccionDao.insertAll(listOf(transaccion))
                getCollection(transaccion.familiaId).document(transaccion.id).set(transaccion)
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar transacción", e)
            }
        }
    }

    suspend fun deleteTransaccion(transaccion: Transaccion) {
        if (transaccion.id.isNotEmpty()) {
            try {
                transaccionDao.delete(transaccion)
                getCollection(transaccion.familiaId).document(transaccion.id).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar transacción", e)
            }
        }
    }

    suspend fun transferPersonalToFamily(userId: String, familiaId: String) {
        try {
            val personalColl = firestore.collection("users").document(userId).collection("transacciones")
            val familyColl = firestore.collection("families").document(familiaId).collection("transacciones")
            
            val snapshots = personalColl.get().await()
            
            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    val trans = doc.toObject(Transaccion::class.java)
                    if (trans != null) {
                        val newTrans = trans.copy(familiaId = familiaId)
                        transaction.set(familyColl.document(doc.id), newTrans)
                        transaction.delete(doc.reference)
                    }
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al transferir transacciones", e)
        }
    }

    suspend fun deleteFamilyData(familiaId: String) {
        try {
            val familyColl = firestore.collection("families").document(familiaId).collection("transacciones")
            val snapshots = familyColl.get().await()
            
            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar datos de familia", e)
        }
    }

    suspend fun deletePersonalData(userId: String) {
        try {
            val personalColl = firestore.collection("users").document(userId).collection("transacciones")
            val snapshots = personalColl.get().await()

            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar datos personales", e)
        }
    }
}
