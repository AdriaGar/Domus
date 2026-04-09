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
                // Aquí podrías abrir una pantalla de edición si lo deseas
            },
            onDeleteClick = { producto -> showDeleteConfirmation(producto) }
        )
    }

    private fun setupFabs() {
        // Navegar a añadir producto
        binding.fabAddStockItem.setOnClickListener {
            findNavController().navigate(R.id.action_f_StockCocina_to_f_AnadirProductoStock)
        }

        // Diálogo para añadir nuevo almacén
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
            .setTitle("Eliminar producto")
            .setMessage("¿Quieres quitar '${producto.nombre}' del stock?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteProducto(producto) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Combinamos productos y almacenes para crear la lista visual organizada
            combine(viewModel.allProductos, viewModel.allAlmacenes) { productos, almacenes ->
                Pair(productos, almacenes)
            }.collect { (productos, almacenes) ->
                
                val displayList = mutableListOf<StockDisplayItem>()
                
                // Agrupamos productos por almacenId
                val productosPorAlmacen = productos.groupBy { it.almacenId }
                
                // 1. Añadimos productos organizados por sus almacenes registrados
                almacenes.forEach { almacen ->
                    val productosEnAlmacen = productosPorAlmacen[almacen.id] ?: emptyList()
                    if (productosEnAlmacen.isNotEmpty()) {
                        displayList.add(StockDisplayItem.Header(almacen.nombre))
                        productosEnAlmacen.forEach { 
                            displayList.add(StockDisplayItem.Product(it)) 
                        }
                    }
                }
                
                // 2. Añadimos productos que no tengan almacén asignado (si los hay)
                val productosSinAlmacen = productos.filter { it.almacenId.isEmpty() }
                if (productosSinAlmacen.isNotEmpty()) {
                    displayList.add(StockDisplayItem.Header("Sin ubicación"))
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
