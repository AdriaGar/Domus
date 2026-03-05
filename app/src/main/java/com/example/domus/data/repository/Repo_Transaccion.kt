package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.TransaccionDao
import com.example.domus.data.Entity.Transaccion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

    private val transaccionesCollection = firestore.collection("transacciones")
    private var snapshotListener: ListenerRegistration? = null

    val allTransacciones: Flow<List<Transaccion>> = transaccionDao.getAll()

    init {
        Log.d(TAG, "Repo_Transaccion inicializado.")
    }

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        
        val userId = auth.currentUser?.uid ?: return
        
        val query = if (familiaId != null) {
            transaccionesCollection.whereEqualTo("familiaId", familiaId)
        } else {
            transaccionesCollection.whereEqualTo("usuarioId", userId).whereEqualTo("familiaId", null)
        }

        snapshotListener = query.addSnapshotListener { snapshots, e ->
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
            val docRef = transaccionesCollection.document()
            val finalTransaccion = transaccion.copy(
                id = docRef.id,
                usuarioId = user.uid,
                familiaId = familiaId
            )
            docRef.set(finalTransaccion).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir transacción", e)
        }
    }

    suspend fun updateTransaccion(transaccion: Transaccion) {
        if (transaccion.id.isNotEmpty()) {
            try {
                transaccionesCollection.document(transaccion.id).set(transaccion).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar transacción", e)
            }
        }
    }

    suspend fun deleteTransaccion(transaccion: Transaccion) {
        if (transaccion.id.isNotEmpty()) {
            try {
                transaccionesCollection.document(transaccion.id).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar transacción", e)
            }
        }
    }

    suspend fun transferPersonalToFamily(userId: String, familiaId: String) {
        try {
            val personalTrans = transaccionesCollection
                .whereEqualTo("usuarioId", userId)
                .whereEqualTo("familiaId", null)
                .get().await()

            firestore.runTransaction { transaction ->
                personalTrans.documents.forEach { doc ->
                    transaction.update(doc.reference, "familiaId", familiaId)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al transferir transacciones", e)
        }
    }

    suspend fun deleteFamilyData(familiaId: String) {
        try {
            val familyTrans = transaccionesCollection
                .whereEqualTo("familiaId", familiaId)
                .get().await()

            firestore.runTransaction { transaction ->
                familyTrans.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar datos de familia", e)
        }
    }

    suspend fun deletePersonalData(userId: String) {
        try {
            val personalTrans = transaccionesCollection
                .whereEqualTo("usuarioId", userId)
                .whereEqualTo("familiaId", null)
                .get().await()

            firestore.runTransaction { transaction ->
                personalTrans.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar datos personales", e)
        }
    }
}
