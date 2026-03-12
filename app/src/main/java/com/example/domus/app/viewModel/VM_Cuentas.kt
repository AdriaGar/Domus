package com.example.domus.app.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domus.data.database.DB_Domus
import com.example.domus.data.Entity.Transaccion
import com.example.domus.data.repository.Repo_Transaccion
import com.example.domus.data.repository.Repo_Familia
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Añadimos lastUpdated para forzar el refresco de caché en Glide
data class User(val uid: String, val nombre: String, val photoUrl: String? = null, val lastUpdated: Long = 0L)

class VM_Cuentas(private val repository: Repo_Transaccion, application: Application) : ViewModel() {

    private val TAG = "DomusDebug"
    private val auth = FirebaseAuth.getInstance()
    private val db = DB_Domus.getDatabase(application)
    private val familiaDao = db.familiaDao()
    
    private var usersListener: ListenerRegistration? = null

    val allTransacciones: Flow<List<Transaccion>> = repository.allTransacciones
    
    private val _currentFamiliaId = MutableStateFlow<String?>(null)
    val currentFamiliaId = _currentFamiliaId.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: Flow<List<User>> = _users.asStateFlow()

    init {
        syncUserProfile()
        observeFamilia()
    }

    private fun syncUserProfile() = viewModelScope.launch {
        val user = auth.currentUser ?: return@launch
        try {
            user.reload().await()
            val updates = mutableMapOf<String, Any>(
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "nombre" to (user.displayName ?: ""),
                "lastUpdated" to System.currentTimeMillis() // Esto fuerza el cambio en Firestore
            )

            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .set(updates, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando perfil: ${e.message}")
        }
    }

    private fun observeFamilia() = viewModelScope.launch {
        familiaDao.getFamilia().collectLatest { familia ->
            _currentFamiliaId.value = familia?.id
            repository.startSync(familia?.id)
            
            usersListener?.remove()

            if (familia != null && familia.members.isNotEmpty()) {
                usersListener = FirebaseFirestore.getInstance().collection("users")
                    .whereIn(FieldPath.documentId(), familia.members)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) return@addSnapshotListener
                        
                        val members = snapshots?.documents?.mapNotNull { doc ->
                            User(
                                uid = doc.id,
                                nombre = doc.getString("nombre") ?: "Usuario",
                                photoUrl = doc.getString("photoUrl"),
                                lastUpdated = doc.getLong("lastUpdated") ?: 0L
                            )
                        } ?: emptyList()
                        
                        _users.value = members
                    }
            } else {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    _users.value = listOf(User(currentUser.uid, currentUser.displayName ?: "Yo", currentUser.photoUrl?.toString()))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
    }

    suspend fun getTransaccionById(id: String): Transaccion? = allTransacciones.firstOrNull()?.find { it.id == id }
    fun addTransaccion(transaccion: Transaccion, familiaId: String?) = viewModelScope.launch { repository.addTransaccion(transaccion, familiaId) }
    fun updateTransaccion(transaccion: Transaccion) = viewModelScope.launch { repository.updateTransaccion(transaccion) }
    fun deleteTransaccion(transaccion: Transaccion) = viewModelScope.launch { repository.deleteTransaccion(transaccion) }
}

class CuentasViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_Cuentas::class.java)) {
            val db = DB_Domus.getDatabase(application)
            return VM_Cuentas(Repo_Transaccion(db.transaccionDao()), application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
