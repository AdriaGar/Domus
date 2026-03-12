package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.app.viewModel.ListaCompraViewModelFactory
import com.example.domus.app.viewModel.VM_ListaCompra
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.FragmentListaCompraBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class F_ListaCompra : Fragment() {

    private var _binding: FragmentListaCompraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_ListaCompra by viewModels {
        ListaCompraViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaCompraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFabs()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvListaCompra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListaCompra.adapter = Adapt_ListaCompra(
            onItemClick = { item -> viewModel.toggleItem(item) },
            onItemLongClick = { item -> showDeleteConfirmation(item) }
        )
    }

    private fun setupFabs() {
        binding.fabAddListaCompraItem.setOnClickListener {
            showAddItemDialog()
        }

        binding.fabClearCompleted.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Limpiar lista")
                .setMessage("¿Quieres eliminar todos los productos marcados como comprados?")
                .setPositiveButton("Eliminar") { _, _ -> viewModel.clearCompletedItems() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allItems.collect { items ->
                (binding.rvListaCompra.adapter as? Adapt_ListaCompra)?.updateData(items)
                
                val pendientes = items.count { !it.comprado }
                val compradosCount = items.count { it.comprado }
                
                binding.tvItemsCount.text = "$pendientes Pendientes"
                
                // Mostrar el botón de limpiar solo si hay items comprados
                binding.fabClearCompleted.isVisible = compradosCount > 0
            }
        }
    }

    private fun showAddItemDialog() {
        val input = EditText(requireContext())
        input.hint = "Ej. Leche, Huevos..."
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir a la lista")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.addItem(nombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(item: Entity_ItemCompra) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar artículo")
            .setMessage("¿Quieres quitar '${item.nombre}' de la lista?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
