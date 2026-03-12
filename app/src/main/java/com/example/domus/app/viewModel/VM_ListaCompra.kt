package com.example.domus.app.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domus.data.database.DB_Domus
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.data.repository.Repo_ListaCompra
import com.example.domus.data.repository.Repo_Familia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VM_ListaCompra(private val repository: Repo_ListaCompra, application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = DB_Domus.getDatabase(application)
    private val familiaDao = db.familiaDao()

    val allItems: Flow<List<Entity_ItemCompra>> = repository.allItems
    
    private val _currentFamiliaId = MutableStateFlow<String?>(null)
    val currentFamiliaId = _currentFamiliaId.asStateFlow()

    init {
        observeFamilia()
    }

    private fun observeFamilia() = viewModelScope.launch {
        familiaDao.getFamilia().collectLatest { familia ->
            _currentFamiliaId.value = familia?.id
            repository.startSync(familia?.id)
        }
    }

    fun addItem(nombre: String) = viewModelScope.launch {
        val item = Entity_ItemCompra(nombre = nombre, comprado = false)
        repository.addItem(item, _currentFamiliaId.value)
    }

    fun toggleItem(item: Entity_ItemCompra) = viewModelScope.launch {
        repository.updateItem(item.copy(comprado = !item.comprado))
    }

    fun deleteItem(item: Entity_ItemCompra) = viewModelScope.launch {
        repository.deleteItem(item)
    }

    fun clearCompletedItems() = viewModelScope.launch {
        val items = allItems.first()
        items.filter { it.comprado }.forEach { item ->
            repository.deleteItem(item)
        }
    }
}

class ListaCompraViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_ListaCompra::class.java)) {
            val db = DB_Domus.getDatabase(application)
            val repository = Repo_ListaCompra(db.itemCompraDao())
            return VM_ListaCompra(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
