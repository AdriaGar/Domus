package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.ItemCompraDao
import com.example.domus.data.Entity.Entity_ItemCompra
import com.google.firebase.auth.FirebaseAuth
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

    private val itemsCollection = firestore.collection("lista_compra")
    private var snapshotListener: ListenerRegistration? = null

    val allItems: Flow<List<Entity_ItemCompra>> = itemDao.getAll()

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        
        val query = if (familiaId != null) {
            itemsCollection.whereEqualTo("familiaId", familiaId)
        } else {
            itemsCollection.whereEqualTo("usuarioId", userId).whereEqualTo("familiaId", null)
        }

        snapshotListener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
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
            val docRef = itemsCollection.document()
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
                itemsCollection.document(item.id).set(item)
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar item compra", e)
            }
        }
    }

    suspend fun deleteItem(item: Entity_ItemCompra) {
        if (item.id.isNotEmpty()) {
            try {
                itemDao.delete(item)
                itemsCollection.document(item.id).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar item compra", e)
            }
        }
    }

    // --- Métodos de gestión de datos para familia ---

    suspend fun transferPersonalToFamily(userId: String, familiaId: String) {
        try {
            val personalItems = itemsCollection
                .whereEqualTo("usuarioId", userId)
                .whereEqualTo("familiaId", null)
                .get().await()

            firestore.runTransaction { transaction ->
                personalItems.documents.forEach { doc ->
                    transaction.update(doc.reference, "familiaId", familiaId)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al transferir lista de compra", e)
        }
    }

    suspend fun deleteFamilyData(familiaId: String) {
        try {
            val familyItems = itemsCollection
                .whereEqualTo("familiaId", familiaId)
                .get().await()

            firestore.runTransaction { transaction ->
                familyItems.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar lista de compra familiar", e)
        }
    }

    suspend fun deletePersonalData(userId: String) {
        try {
            val personalItems = itemsCollection
                .whereEqualTo("usuarioId", userId)
                .whereEqualTo("familiaId", null)
                .get().await()

            firestore.runTransaction { transaction ->
                personalItems.documents.forEach { doc ->
                    transaction.delete(doc.reference)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar lista de compra personal", e)
        }
    }
}
