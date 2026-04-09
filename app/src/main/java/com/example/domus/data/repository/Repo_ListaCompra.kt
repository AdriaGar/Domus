package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.ItemCompraDao
import com.example.domus.data.Entity.Entity_ItemCompra
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

class Repo_ListaCompra(private val itemDao: ItemCompraDao) {

    private val TAG = "DomusDebug"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var snapshotListener: ListenerRegistration? = null

    val allItems: Flow<List<Entity_ItemCompra>> = itemDao.getAll()

    private fun getCollection(familiaId: String?): CollectionReference {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return if (familiaId != null) {
            firestore.collection("families").document(familiaId).collection("lista_compra")
        } else {
            firestore.collection("users").document(userId).collection("lista_compra")
        }
    }

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        
        val collection = try { getCollection(familiaId) } catch (e: Exception) { return }

        snapshotListener = collection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de ListaCompra", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val items = snapshots.toObjects(Entity_ItemCompra::class.java)
                repositoryScope.launch {
                    try {
                        itemDao.deleteAll()
                        itemDao.insertAll(items)
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error al actualizar Room ListaCompra", dbError)
                    }
                }
            }
        }
    }

    suspend fun addItem(item: Entity_ItemCompra, familiaId: String?) {
        val user = auth.currentUser ?: return
        try {
            val collection = getCollection(familiaId)
            val docRef = collection.document()
            val finalItem = item.copy(
                id = docRef.id,
                usuarioId = user.uid,
                familiaId = familiaId
            )
            itemDao.insertAll(listOf(finalItem))
            docRef.set(finalItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir item compra", e)
        }
    }

    suspend fun updateItem(item: Entity_ItemCompra) {
        if (item.id.isNotEmpty()) {
            try {
                itemDao.insertAll(listOf(item))
                getCollection(item.familiaId).document(item.id).set(item)
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar item compra", e)
            }
        }
    }

    suspend fun deleteItem(item: Entity_ItemCompra) {
        if (item.id.isNotEmpty()) {
            try {
                itemDao.delete(item)
                getCollection(item.familiaId).document(item.id).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar item compra", e)
            }
        }
    }

    suspend fun transferPersonalToFamily(userId: String, familiaId: String) {
        try {
            val personalColl = firestore.collection("users").document(userId).collection("lista_compra")
            val familyColl = firestore.collection("families").document(familiaId).collection("lista_compra")
            
            val snapshots = personalColl.get().await()

            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    val item = doc.toObject(Entity_ItemCompra::class.java)
                    if (item != null) {
                        val newItem = item.copy(familiaId = familiaId)
                        transaction.set(familyColl.document(doc.id), newItem)
                        transaction.delete(doc.reference)
                    }
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al transferir lista de compra", e)
        }
    }

    suspend fun deleteFamilyData(familiaId: String) {
        try {
            val familyColl = firestore.collection("families").document(familiaId).collection("lista_compra")
            val snapshots = familyColl.get().await()

            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar lista de compra familiar", e)
        }
    }

    suspend fun deletePersonalData(userId: String) {
        try {
            val personalColl = firestore.collection("users").document(userId).collection("lista_compra")
            val snapshots = personalColl.get().await()

            firestore.runTransaction { transaction ->
                snapshots.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar lista de compra personal", e)
        }
    }
}
