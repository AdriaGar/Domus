package com.example.domus.data.repository

import android.util.Log
import com.example.domus.data.database.TransaccionDao
import com.example.domus.data.Entity.Transaccion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    private val idGrupoCuentas = "grupo_principal"
    private val transaccionesCollection = firestore.collection("grupos").document(idGrupoCuentas).collection("transacciones")

    val allTransacciones: Flow<List<Transaccion>> = transaccionDao.getAll()

    init {
        Log.d(TAG, "Repo_Transaccion inicializado. Iniciando sincronización.")
        syncTransacciones()
    }

    suspend fun addTransaccion(transaccion: Transaccion) {
        if (auth.currentUser != null) {
            try {
                Log.d(TAG, "Añadiendo transacción a Firestore: $transaccion")
                val documentReference = transaccionesCollection.document()
                val transaccionConId = transaccion.copy(id = documentReference.id)
                documentReference.set(transaccionConId).await()
                Log.d(TAG, "Transacción añadida a Firestore con éxito. ID: ${documentReference.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir la transacción a Firestore", e)
            }
        } else {
            Log.w(TAG, "No se puede añadir transacción, usuario no autenticado")
        }
    }

    suspend fun updateTransaccion(transaccion: Transaccion) {
        if (auth.currentUser != null && transaccion.id.isNotEmpty()) {
            try {
                Log.d(TAG, "Actualizando transacción en Firestore: ${transaccion.id}")
                // Usamos el ID existente para sobrescribir el documento en Firestore
                transaccionesCollection.document(transaccion.id).set(transaccion).await()
                Log.d(TAG, "Transacción actualizada con éxito.")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar la transacción en Firestore", e)
            }
        }
    }

    private fun syncTransacciones() {
        transaccionesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e(TAG, "Error en el listener de Firestore.", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val transacciones = snapshots.toObjects(Transaccion::class.java)
                Log.d(TAG, "Firestore ha devuelto ${transacciones.size} transacciones. Sincronizando con Room...")
                repositoryScope.launch {
                    try {
                        transaccionDao.deleteAll()
                        transaccionDao.insertAll(transacciones)
                        Log.d(TAG, "Room actualizado con éxito.")
                    } catch(dbError: Exception) {
                        Log.e(TAG, "Error al actualizar la base de datos de Room", dbError)
                    }
                }
            }
        }
    }
}