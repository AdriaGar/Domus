package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.StockCocinaDao
import com.example.domus.app.Producto
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

class Repo_StockCocina(private val stockDao: StockCocinaDao) {

    private val TAG = "DomusDebug"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val stockCollection = firestore.collection("stock_cocina")
    private var snapshotListener: ListenerRegistration? = null

    val allProductos: Flow<List<Producto>> = stockDao.getAll()

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        val userId = auth.currentUser?.uid ?: return
        
        val query = if (familiaId != null) {
            stockCollection.whereEqualTo("familiaId", familiaId)
        } else {
            stockCollection.whereEqualTo("usuarioId", userId).whereEqualTo("familiaId", null)
        }

        snapshotListener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de StockCocina", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val productos = snapshots.toObjects(Producto::class.java)
                repositoryScope.launch {
                    try {
                        stockDao.deleteAll()
                        stockDao.insertAll(productos)
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error al actualizar Room StockCocina", dbError)
                    }
                }
            }
        }
    }

    suspend fun addProducto(producto: Producto, familiaId: String?) {
        val user = auth.currentUser ?: return
        try {
            val docRef = stockCollection.document()
            val finalProducto = producto.copy(
                id = docRef.id,
                usuarioId = user.uid,
                familiaId = familiaId
            )
            stockDao.insertAll(listOf(finalProducto))
            docRef.set(finalProducto)
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir producto al stock", e)
        }
    }

    suspend fun updateProducto(producto: Producto) {
        if (producto.id.isNotEmpty()) {
            try {
                stockDao.insertAll(listOf(producto))
                stockCollection.document(producto.id).set(producto)
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar producto del stock", e)
            }
        }
    }

    suspend fun deleteProducto(producto: Producto) {
        if (producto.id.isNotEmpty()) {
            try {
                stockDao.delete(producto)
                stockCollection.document(producto.id).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar producto del stock", e)
            }
        }
    }

    // --- Métodos de gestión de datos familiares ---

    suspend fun transferPersonalToFamily(userId: String, familiaId: String) {
        try {
            val personalItems = stockCollection
                .whereEqualTo("usuarioId", userId)
                .whereEqualTo("familiaId", null)
                .get().await()

            firestore.runTransaction { transaction ->
                personalItems.documents.forEach { doc ->
                    transaction.update(doc.reference, "familiaId", familiaId)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al transferir stock", e)
        }
    }

    suspend fun deleteFamilyData(familiaId: String) {
        try {
            val familyItems = stockCollection.whereEqualTo("familiaId", familiaId).get().await()
            firestore.runTransaction { transaction ->
                familyItems.documents.forEach { transaction.delete(it.reference) }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar stock familiar", e)
        }
    }

    suspend fun deletePersonalData(userId: String) {
        try {
            val personalItems = stockCollection.whereEqualTo("usuarioId", userId).whereEqualTo("familiaId", null).get().await()
            firestore.runTransaction { transaction ->
                personalItems.documents.forEach { transaction.delete(it.reference) }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al borrar stock personal", e)
        }
    }
}
