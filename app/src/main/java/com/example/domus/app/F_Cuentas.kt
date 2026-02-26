package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.databinding.FragmentCuentasBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class F_Cuentas : Fragment() {

    private var _binding: FragmentCuentasBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels para compartir el ViewModel con F_AnadirGasto
    private val viewModel: VM_Cuentas by activityViewModels {
        CuentasViewModelFactory(requireActivity().application)
    }

    private lateinit var transaccionAdapter: Adapt_Transaccion

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCuentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        transaccionAdapter = Adapt_Transaccion()
        binding.rvTransacciones.apply {
            adapter = transaccionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddTransaccion.setOnClickListener {
            // Muestra el diálogo para añadir un nuevo gasto
            F_AnadirGasto().show(parentFragmentManager, "AnadirGastoDialog")
        }
    }

    private fun observeViewModel() {
        // Usamos viewLifecycleOwner.lifecycleScope para que la corutina se cancele automáticamente
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allTransacciones.collectLatest { transacciones ->
                // Cuando llegan nuevas transacciones, las enviamos al adaptador
                transaccionAdapter.submitList(transacciones)

                // Calculamos y mostramos el balance total correctamente
                val balance = transacciones.sumOf { 
                    when (it.tipo) {
                        TipoTransaccion.INGRESO.name -> it.cantidad
                        TipoTransaccion.GASTO.name -> -it.cantidad
                        else -> 0.0 // Las transferencias no afectan al balance total
                    }
                }
                binding.tvBalance.text = String.format("%.2f €", balance)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
