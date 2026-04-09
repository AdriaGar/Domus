package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.StockCocinaDao
import com.example.domus.app.Producto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
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

    private var snapshotListener: ListenerRegistration? = null

    val allProductos: Flow<List<Producto>> = stockDao.getAll()

    private fun getCollection(familiaId: String?): CollectionReference {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return if (!familiaId.isNullOrEmpty()) {
            firestore.collection("families").document(familiaId).collection("stock_cocina")
        } else {
            firestore.collection("users").document(userId).collection("stock_cocina")
        }
    }

    fun startSync(familiaId: String?) {
        snapshotListener?.remove()
        
        val collection = try { getCollection(familiaId) } catch (e: Exception) { return }

        snapshotListener = collection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de StockCocina", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                repositoryScope.launch {
                    try {
                        val stockItems = snapshots.documents.map { doc ->
                            val barcode = doc.getString("barcode")
                            val productId = doc.getString("productId") ?: barcode ?: ""
                            val cantidad = doc.getLong("cantidad")?.toInt() ?: 0
                            val almacenId = doc.getString("almacenId") ?: ""
                            
                            var finalProducto = Producto(
                                id = doc.id,
                                barcode = barcode,
                                cantidad = cantidad,
                                almacenId = almacenId,
                                familiaId = familiaId
                            )

                            // Lookup de datos globales
                            val lookupId = if (!barcode.isNullOrEmpty()) barcode else productId
                            if (lookupId.isNotEmpty()) {
                                try {
                                    val globalDoc = firestore.collection("productos_globales")
                                        .document(lookupId)
                                        .get()
                                        .await()
                                    
                                    if (globalDoc.exists()) {
                                        finalProducto = finalProducto.copy(
                                            nombre = globalDoc.getString("nombre") ?: "",
                                            marca = globalDoc.getString("marca") ?: "",
                                            categoria = globalDoc.getString("categoria") ?: "",
                                            fotoBase64 = globalDoc.getString("fotoBase64"),
                                            energia = globalDoc.getString("energia"),
                                            grasas = globalDoc.getString("grasas"),
                                            grasasSaturadas = globalDoc.getString("grasasSaturadas"),
                                            hidratos = globalDoc.getString("hidratos"),
                                            azucares = globalDoc.getString("azucares"),
                                            proteinas = globalDoc.getString("proteinas"),
                                            sal = globalDoc.getString("sal")
                                        )
                                    }
                                } catch (ge: Exception) {
                                    Log.e(TAG, "Error lookup global: $lookupId", ge)
                                }
                            }
                            finalProducto
                        }
                        
                        stockDao.deleteAll()
                        stockDao.insertAll(stockItems)
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error Room: ", dbError)
                    }
                }
            }
        }
    }

    suspend fun addProducto(producto: Producto, familiaId: String?) {
        val user = auth.currentUser ?: return
        try {
            // 1. ID Global
            val globalProductId = if (!producto.barcode.isNullOrEmpty()) producto.barcode else firestore.collection("productos_globales").document().id

            // 2. Guardar datos técnicos (incluida fotoBase64) en RAÍZ
            val globalData = mapOf(
                "nombre" to producto.nombre,
                "marca" to producto.marca,
                "categoria" to producto.categoria,
                "barcode" to producto.barcode,
                "fotoBase64" to producto.fotoBase64,
                "energia" to producto.energia,
                "grasas" to producto.grasas,
                "grasasSaturadas" to producto.grasasSaturadas,
                "hidratos" to producto.hidratos,
                "azucares" to producto.azucares,
                "proteinas" to producto.proteinas,
                "sal" to producto.sal
            )
            firestore.collection("productos_globales").document(globalProductId).set(globalData, SetOptions.merge()).await()

            // 3. Guardar en la FAMILIA/USUARIO (Stock real)
            val collection = getCollection(familiaId)
            val docRef = collection.document()
            
            val stockData = mapOf(
                "productId" to globalProductId,
                "barcode" to producto.barcode,
                "cantidad" to producto.cantidad,
                "almacenId" to producto.almacenId,
                "usuarioId" to user.uid,
                "familiaId" to familiaId
            )
            
            docRef.set(stockData).await()
            Log.d(TAG, "Guardado completo: Global + Stock")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico al guardar", e)
        }
    }

    suspend fun updateProducto(producto: Producto) {
        if (producto.id.isNotEmpty()) {
            try {
                val stockUpdate = mapOf(
                    "cantidad" to producto.cantidad,
                    "almacenId" to producto.almacenId
                )
                getCollection(producto.familiaId).document(producto.id).update(stockUpdate).await()
                
                // Si hay barcode, actualizamos también lo global por si cambió la foto o info
                if (!producto.barcode.isNullOrEmpty()) {
                    saveToGlobalProducts(producto)
                }
                stockDao.insertAll(listOf(producto))
            } catch (e: Exception) {
                Log.e(TAG, "Error update", e)
            }
        }
    }

    private suspend fun saveToGlobalProducts(producto: Producto) {
        val id = if (!producto.barcode.isNullOrEmpty()) producto.barcode else return
        try {
            val data = mapOf(
                "nombre" to producto.nombre,
                "marca" to producto.marca,
                "fotoBase64" to producto.fotoBase64,
                "energia" to producto.energia,
                "grasas" to producto.grasas,
                "grasasSaturadas" to producto.grasasSaturadas,
                "hidratos" to producto.hidratos,
                "azucares" to producto.azucares,
                "proteinas" to producto.proteinas,
                "sal" to producto.sal
            )
            firestore.collection("productos_globales").document(id).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error global update", e)
        }
    }

    suspend fun deleteProducto(producto: Producto) {
        if (producto.id.isNotEmpty()) {
            try {
                stockDao.delete(producto)
                getCollection(producto.familiaId).document(producto.id).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error delete", e)
            }
        }
    }
}
