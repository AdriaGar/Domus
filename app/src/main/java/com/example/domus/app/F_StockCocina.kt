package com.example.domus.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.R
import com.example.domus.app.viewModel.StockCocinaViewModelFactory
import com.example.domus.app.viewModel.VM_StockCocina
import com.example.domus.databinding.FragmentStockCocinaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class F_StockCocina : Fragment() {

    private var _binding: FragmentStockCocinaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_StockCocina by viewModels {
        StockCocinaViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockCocinaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFabs()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvStockCocina.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStockCocina.adapter = Adapt_StockCocina(
            onItemClick = { producto -> 
                val bundle = Bundle().apply {
                    putString("productoId", producto.id)
                    putString("title", "Editar Producto")
                }
                findNavController().navigate(R.id.action_f_StockCocina_to_f_AnadirProductoStock, bundle)
            },
            onPlusClick = { producto -> viewModel.sumarCantidad(producto) },
            onMinusClick = { producto -> 
                viewModel.restarCantidad(producto) { prod ->
                    showDeleteConfirmation(prod)
                }
            },
            onHeaderClick = { almacen -> viewModel.toggleAlmacen(almacen.id) },
            onAlmacenEdit = { almacen -> showEditAlmacenDialog(almacen) },
            onAlmacenDelete = { almacen -> showDeleteAlmacenDialog(almacen) }
        )
    }

    private fun showEditAlmacenDialog(almacen: Almacen) {
        val input = EditText(requireContext())
        input.setText(almacen.nombre)
        input.hint = "Nuevo nombre"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar Almacén")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = input.text.toString().trim()
                if (nuevoNombre.isNotEmpty()) {
                    viewModel.renameAlmacen(almacen, nuevoNombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteAlmacenDialog(almacen: Almacen) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Almacén")
            .setMessage("¿Estás seguro de que quieres eliminar '${almacen.nombre}'? Los productos no se borrarán, pero se quedarán en la sección 'Desconocido'.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteAlmacen(almacen)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupFabs() {
        binding.fabAddStockItem.setOnClickListener {
            findNavController().navigate(R.id.action_f_StockCocina_to_f_AnadirProductoStock)
        }

        binding.fabAddAlmacen.setOnClickListener {
            showAddAlmacenDialog()
        }
    }

    private fun showAddAlmacenDialog() {
        val input = EditText(requireContext())
        input.hint = "Ej. Nevera, Despensa..."
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo Almacén")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.addAlmacen(nombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(producto: Producto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Eliminar ${producto.nombre}?")
            .setMessage("El producto se agotará. ¿Quieres añadirlo a la lista de la compra?")
            .setNeutralButton("Solo eliminar") { _, _ -> 
                viewModel.deleteProducto(producto, addToShoppingList = false) 
            }
            .setPositiveButton("Añadir a la lista") { _, _ -> 
                viewModel.deleteProducto(producto, addToShoppingList = true) 
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.allProductos, 
                viewModel.allAlmacenes, 
                viewModel.expandedAlmacenes
            ) { productos, almacenes, expanded ->
                Triple(productos, almacenes, expanded)
            }.collect { (productos, almacenes, expanded) ->
                
                val displayList = mutableListOf<StockDisplayItem>()
                val productosPorAlmacen = productos.groupBy { it.almacenId }
                
                // 1. Mostrar almacenes reales (ordenados por nombre o como vengan)
                almacenes.forEach { almacen ->
                    val productosEnAlmacen = productosPorAlmacen[almacen.id] ?: emptyList()
                    val isExpanded = expanded.contains(almacen.id)
                    
                    displayList.add(StockDisplayItem.Header(almacen, isExpanded, productosEnAlmacen.size))
                    
                    if (isExpanded) {
                        productosEnAlmacen.forEach { 
                            displayList.add(StockDisplayItem.Product(it)) 
                        }
                    }
                }
                
                // 2. Mostrar sección "Desconocido" para productos sin almacén asignado
                val productosSinAlmacen = productos.filter { it.almacenId.isEmpty() || !almacenes.any { a -> a.id == it.almacenId } }
                val dummyAlmacen = Almacen(id = "desconocido", nombre = "Desconocido")
                val isExpandedDesconocido = expanded.contains(dummyAlmacen.id)
                
                // Siempre mostramos la sección Desconocido al final
                displayList.add(StockDisplayItem.Header(dummyAlmacen, isExpandedDesconocido, productosSinAlmacen.size))
                
                if (isExpandedDesconocido) {
                    productosSinAlmacen.forEach { 
                        displayList.add(StockDisplayItem.Product(it)) 
                    }
                }

                (binding.rvStockCocina.adapter as? Adapt_StockCocina)?.submitList(displayList)
                binding.tvStockCount.text = "${productos.size} Productos"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
