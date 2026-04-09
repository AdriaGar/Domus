package com.example.domus.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.app.viewModel.ListaCompraViewModelFactory
import com.example.domus.app.viewModel.User
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.app.viewModel.VM_ListaCompra
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.FragmentHistorialCompraBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class F_HistorialCompra : Fragment() {

    private var _binding: FragmentHistorialCompraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_ListaCompra by viewModels {
        ListaCompraViewModelFactory(requireActivity().application)
    }
    
    private val accountsViewModel: VM_Cuentas by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialCompraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupHeaderClick()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvHistorialCompra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialCompra.adapter = Adapt_HistorialCompra(
            onItemClick = { item -> showRestoreConfirmation(item) },
            onItemLongClick = { item -> showDeleteConfirmation(item) }
        )
    }

    private fun setupHeaderClick() {
        binding.tvHistorialCount.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.allItems, accountsViewModel.users) { items: List<Entity_ItemCompra>, users: List<User> ->
                Pair(items, users)
            }.collect { (items, users) ->
                val archivedItems = items.filter { it.archivado }
                
                // Agrupamos los artículos por loteId y los ordenamos por fecha descendente
                val groupedItems = archivedItems
                    .filter { it.loteId != null && it.fechaLiquidacion != null }
                    .groupBy { it.loteId }
                    .toList()
                    .sortedByDescending { it.second.first().fechaLiquidacion }

                // Convertimos a una lista plana de HistorialItem (Cabecera + Productos)
                val flatList = mutableListOf<HistorialItem>()
                groupedItems.forEach { (loteId, products) ->
                    val fecha = products.first().fechaLiquidacion ?: 0L
                    flatList.add(HistorialItem.Header(fecha))
                    products.forEach { product ->
                        val user = users.find { it.uid == product.usuarioId }
                        flatList.add(HistorialItem.Product(product, user))
                    }
                }
                
                val adapter = binding.rvHistorialCompra.adapter as? Adapt_HistorialCompra
                adapter?.submitList(flatList)
                
                binding.tvHistorialCount.text = "${archivedItems.size} Artículos"
            }
        }
    }

    private fun showRestoreConfirmation(item: Entity_ItemCompra) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restaurar artículo")
            .setMessage("¿Quieres devolver '${item.nombre}' a la lista de la compra?")
            .setPositiveButton("Restaurar") { _, _ -> viewModel.restoreItem(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmation(item: Entity_ItemCompra) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar permanentemente")
            .setMessage("¿Estás seguro de que quieres borrar '${item.nombre}' del historial?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
