package com.example.domus.app.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domus.data.database.DB_Domus
import com.example.domus.app.Producto
import com.example.domus.app.Almacen
import com.example.domus.data.repository.Repo_StockCocina
import com.example.domus.data.repository.Repo_Almacen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VM_StockCocina(
    private val repository: Repo_StockCocina, 
    private val repoAlmacen: Repo_Almacen,
    application: Application
) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = DB_Domus.getDatabase(application)
    private val familiaDao = db.familiaDao()
    private val firestore = FirebaseFirestore.getInstance()

    val allProductos: Flow<List<Producto>> = repository.allProductos
    val allAlmacenes: Flow<List<Almacen>> = repoAlmacen.allAlmacenes
    
    private val _currentFamiliaId = MutableStateFlow<String?>(null)
    val currentFamiliaId = _currentFamiliaId.asStateFlow()

    init {
        observeFamilia()
    }

    private fun observeFamilia() = viewModelScope.launch {
        familiaDao.getFamilia().collectLatest { familia ->
            _currentFamiliaId.value = familia?.id
            repository.startSync(familia?.id)
            repoAlmacen.startSync(familia?.id)
        }
    }

    suspend fun findProductByBarcode(barcode: String): Producto? {
        val localMatch = allProductos.first().find { it.barcode == barcode }
        if (localMatch != null) return localMatch

        return try {
            val globalSnapshot = firestore.collection("productos_globales")
                .whereEqualTo("barcode", barcode)
                .limit(1)
                .get()
                .await()
            
            if (!globalSnapshot.isEmpty) {
                globalSnapshot.documents[0].toObject(Producto::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addProducto(
        nombre: String, 
        marca: String, 
        cantidad: Int, 
        almacenId: String,
        categoria: String = "",
        infoNutricional: String? = null,
        fotoUrl: String? = null,
        barcode: String? = null
    ) = viewModelScope.launch {
        val producto = Producto(
            nombre = nombre,
            marca = marca,
            cantidad = cantidad,
            almacenId = almacenId,
            categoria = categoria,
            infoNutricional = infoNutricional,
            fotoUrl = fotoUrl,
            barcode = barcode
        )
        repository.addProducto(producto, _currentFamiliaId.value)
    }

    fun addAlmacen(nombre: String) = viewModelScope.launch {
        val almacen = Almacen(nombre = nombre)
        repoAlmacen.addAlmacen(almacen, _currentFamiliaId.value)
    }

    fun updateProducto(producto: Producto) = viewModelScope.launch {
        repository.updateProducto(producto)
    }

    fun deleteProducto(producto: Producto) = viewModelScope.launch {
        repository.deleteProducto(producto)
    }
}

class StockCocinaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_StockCocina::class.java)) {
            val db = DB_Domus.getDatabase(application)
            val repoStock = Repo_StockCocina(db.stockCocinaDao())
            val repoAlmacen = Repo_Almacen(db.almacenDao())
            return VM_StockCocina(repoStock, repoAlmacen, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
