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
import com.example.domus.data.repository.Repo_ListaCompra
import com.example.domus.data.Entity.Entity_ItemCompra
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
    private val repoLista: Repo_ListaCompra,
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

    // Estado de expansión de los almacenes
    private val _expandedAlmacenes = MutableStateFlow<Set<String>>(emptySet())
    val expandedAlmacenes = _expandedAlmacenes.asStateFlow()

    init {
        observeFamilia()
    }

    private fun observeFamilia() = viewModelScope.launch {
        familiaDao.getFamilia().collectLatest { familia ->
            if (familia != null) {
                _currentFamiliaId.value = familia.id
                repository.startSync(familia.id)
                repoAlmacen.startSync(familia.id)
            } else {
                try {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        val familyId = userDoc.getString("familyId")
                        _currentFamiliaId.value = familyId
                        repository.startSync(familyId)
                        repoAlmacen.startSync(familyId)
                    }
                } catch (e: Exception) {
                    repository.startSync(null)
                    repoAlmacen.startSync(null)
                }
            }
        }
    }

    fun toggleAlmacen(almacenId: String) {
        val current = _expandedAlmacenes.value
        _expandedAlmacenes.value = if (current.contains(almacenId)) {
            current - almacenId
        } else {
            current + almacenId
        }
    }

    fun deleteAlmacen(almacen: Almacen) = viewModelScope.launch {
        repoAlmacen.deleteAlmacen(almacen)
    }

    fun renameAlmacen(almacen: Almacen, nuevoNombre: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val updated = almacen.copy(nombre = nuevoNombre)
        // El repo actual no tiene un updateAlmacen explícito, pero addAlmacen usa set() en Firestore.
        // Sin embargo, el repo actual genera un nuevo ID. Necesitamos modificar el repo o usar firestore directamente.
        firestore.collection(if (almacen.familiaId != null) "families/${almacen.familiaId}/almacenes" else "users/$uid/almacenes")
            .document(almacen.id)
            .update("nombre", nuevoNombre)
            .await()
    }

    suspend fun findProductByBarcode(barcode: String): Producto? {
        val localMatch = allProductos.first().find { it.barcode == barcode }
        if (localMatch != null) return localMatch

        return try {
            val globalDoc = firestore.collection("productos_globales")
                .document(barcode)
                .get()
                .await()
            
            if (globalDoc.exists()) {
                globalDoc.toObject(Producto::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addProductoCompleto(
        nombre: String, marca: String, cantidad: Double, almacenId: String,
        categoria: String = "", barcode: String? = null,
        energia: String? = null, grasas: String? = null, grasasSaturadas: String? = null,
        hidratos: String? = null, azucares: String? = null, proteinas: String? = null,
        sal: String? = null, fotoBase64: String? = null
    ) = viewModelScope.launch {
        val producto = Producto(
            nombre = nombre, marca = marca, cantidad = cantidad, almacenId = almacenId,
            categoria = categoria, barcode = barcode,
            energia = energia, grasas = grasas, grasasSaturadas = grasasSaturadas,
            hidratos = hidratos, azucares = azucares, proteinas = proteinas,
            sal = sal, fotoBase64 = fotoBase64
        )
        repository.addProducto(producto, _currentFamiliaId.value)
    }

    fun sumarCantidad(producto: Producto) = viewModelScope.launch {
        val current = producto.cantidad
        val nuevaCant = if (current < 1.0) current + 0.25 else current + 1.0
        repository.updateProducto(producto.copy(cantidad = nuevaCant))
    }

    fun restarCantidad(producto: Producto, onDeleteRequested: (Producto) -> Unit) = viewModelScope.launch {
        val current = producto.cantidad
        val nuevaCant = when {
            current > 1.0 -> current - 1.0
            current > 0.25 -> current - 0.25
            else -> {
                onDeleteRequested(producto)
                return@launch
            }
        }
        repository.updateProducto(producto.copy(cantidad = nuevaCant))
    }

    fun deleteProducto(producto: Producto, addToShoppingList: Boolean) = viewModelScope.launch {
        repository.deleteProducto(producto)
        if (addToShoppingList) {
            val item = Entity_ItemCompra(nombre = producto.nombre)
            repoLista.addItem(item, _currentFamiliaId.value)
        }
    }

    fun addAlmacen(nombre: String) = viewModelScope.launch {
        repoAlmacen.addAlmacen(Almacen(nombre = nombre), _currentFamiliaId.value)
    }

    fun updateProducto(producto: Producto) = viewModelScope.launch {
        repository.updateProducto(producto)
    }
}

@Suppress("UNCHECKED_CAST")
class StockCocinaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VM_StockCocina::class.java)) {
            val db = DB_Domus.getDatabase(application)
            val repoStock = Repo_StockCocina(db.stockCocinaDao())
            val repoAlmacen = Repo_Almacen(db.almacenDao())
            val repoLista = Repo_ListaCompra(db.itemCompraDao())
            return VM_StockCocina(repoStock, repoAlmacen, repoLista, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
