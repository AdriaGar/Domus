package com.example.domus.app.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domus.data.database.DB_Domus
import com.example.domus.data.Entity.Transaccion
import com.example.domus.data.repository.Repo_Transaccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Clase de datos simple para representar a un usuario en la UI
data class User(val uid: String, val nombre: String)

class VM_Cuentas(private val repository: Repo_Transaccion) : ViewModel() {

    private val TAG = "DomusDebug"

    // Flow para las transacciones (desde la fuente de verdad, Room)
    val allTransacciones: Flow<List<Transaccion>> = repository.allTransacciones

    // --- GESTIÓN DE USUARIOS (SIMULADA) ---
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: Flow<List<User>> = _users.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        _users.value = listOf(
            User("uid_juan", "Juan"),
            User("uid_ana", "Ana"),
            User("uid_pedro", "Pedro")
        )
    }
    // --- FIN DE LA GESTIÓN DE USUARIOS ---

    suspend fun getTransaccionById(id: String): Transaccion? {
        return allTransacciones.firstOrNull()?.find { it.id == id }
    }

    fun addTransaccion(transaccion: Transaccion) = viewModelScope.launch {
        Log.d(TAG, "Función addTransaccion en VM_Cuentas. Pasando al repositorio...")
        repository.addTransaccion(transaccion)
    }

    fun updateTransaccion(transaccion: Transaccion) = viewModelScope.launch {
        Log.d(TAG, "Función updateTransaccion en VM_Cuentas. Pasando al repositorio...")
        repository.updateTransaccion(transaccion)
    }
}

class CuentasViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_Cuentas::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val db = DB_Domus.getDatabase(application)
            val repository = Repo_Transaccion(db.transaccionDao())
            return VM_Cuentas(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
