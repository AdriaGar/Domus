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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class User(val uid: String, val nombre: String)

class VM_Cuentas(private val repository: Repo_Transaccion, application: Application) : ViewModel() {

    private val TAG = "DomusDebug"
    private val auth = FirebaseAuth.getInstance()
    private val db = DB_Domus.getDatabase(application)
    private val familiaDao = db.familiaDao()
    private val repoFamilia = Repo_Familia()

    // Flow para las transacciones (desde la fuente de verdad, Room)
    val allTransacciones: Flow<List<Transaccion>> = repository.allTransacciones
    
    private val _currentFamiliaId = MutableStateFlow<String?>(null)
    val currentFamiliaId = _currentFamiliaId.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: Flow<List<User>> = _users.asStateFlow()

    init {
        observeFamilia()
    }

    private fun observeFamilia() = viewModelScope.launch {
        familiaDao.getFamilia().collectLatest { familia ->
            _currentFamiliaId.value = familia?.id
            repository.startSync(familia?.id)
            
            if (familia != null) {
                // Cargar miembros reales de la familia
                val membersInfo = repoFamilia.getMembersDetails(familia.members)
                _users.value = membersInfo.map { User(it.id, it.nombre) }
            } else {
                // Si no hay familia, solo el usuario actual
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val nombre = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Yo"
                    _users.value = listOf(User(currentUser.uid, nombre))
                }
            }
        }
    }

    suspend fun getTransaccionById(id: String): Transaccion? {
        return allTransacciones.firstOrNull()?.find { it.id == id }
    }

    fun addTransaccion(transaccion: Transaccion) = viewModelScope.launch {
        Log.d(TAG, "Añadiendo transacción con familiaId: ${_currentFamiliaId.value}")
        repository.addTransaccion(transaccion, _currentFamiliaId.value)
    }

    fun updateTransaccion(transaccion: Transaccion) = viewModelScope.launch {
        repository.updateTransaccion(transaccion)
    }

    fun deleteTransaccion(transaccion: Transaccion) = viewModelScope.launch {
        repository.deleteTransaccion(transaccion)
    }
}

class CuentasViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_Cuentas::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val db = DB_Domus.getDatabase(application)
            val repository = Repo_Transaccion(db.transaccionDao())
            return VM_Cuentas(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
