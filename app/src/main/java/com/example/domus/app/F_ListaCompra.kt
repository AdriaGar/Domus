package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.R
import com.example.domus.app.viewModel.ListaCompraViewModelFactory
import com.example.domus.app.viewModel.User
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.app.viewModel.VM_ListaCompra
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.FragmentListaCompraBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class F_ListaCompra : Fragment() {

    private var _binding: FragmentListaCompraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_ListaCompra by viewModels {
        ListaCompraViewModelFactory(requireActivity().application)
    }
    
    private val accountsViewModel: VM_Cuentas by activityViewModels()

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
        setupHeaderClick()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvListaCompra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListaCompra.adapter = Adapt_ListaCompra(
            onItemClick = { item -> viewModel.toggleItem(item) },
            onItemLongClick = { item -> showDeleteConfirmation(item) }
        )
    }

    private fun setupHeaderClick() {
        binding.tvItemsCount.setOnClickListener {
            findNavController().navigate(R.id.f_HistorialCompra)
        }
    }

    private fun setupFabs() {
        binding.fabAddListaCompraItem.setOnClickListener {
            showAddItemDialog()
        }

        binding.fabClearCompleted.setOnClickListener {
            showLiquidationDialog()
        }
    }

    private fun showLiquidationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Liquidar Compra")
            .setMessage("¿Quieres registrar un gasto en cuentas además de archivar estos productos?")
            .setPositiveButton("Liquidar y Añadir Gasto") { _, _ ->
                viewModel.archiveCompletedItems()
                // Navegamos a añadir gasto para registrar el importe de la compra
                val action = F_ListaCompraDirections.actionFListaCompraToFAnadirGasto(
                    transaccionId = null,
                    title = "Registrar Gasto de Compra"
                )
                findNavController().navigate(action)
            }
            .setNeutralButton("Solo Liquidar") { _, _ ->
                viewModel.archiveCompletedItems()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.allItems, accountsViewModel.users) { items: List<Entity_ItemCompra>, users: List<User> ->
                Pair(items, users)
            }.collect { (items, users) ->
                val currentItems = items.filter { !it.archivado }
                
                val adapter = binding.rvListaCompra.adapter as? Adapt_ListaCompra
                adapter?.let {
                    it.updateUsers(users)
                    it.submitList(currentItems)
                }
                
                val pendientes = currentItems.count { !it.comprado }
                val compradosCount = currentItems.count { it.comprado }
                
                binding.tvItemsCount.text = "$pendientes Pendientes"
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
            .setMessage("¿Quieres eliminar permanentemente '${item.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
