package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.R
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.databinding.FragmentCuentasBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class F_Cuentas : Fragment() {

    private var _binding: FragmentCuentasBinding? = null
    private val binding get() = _binding!!

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
            // Navegar al nuevo fragmento de pantalla completa
            findNavController().navigate(R.id.action_f_Cuentas_to_f_AnadirGasto)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allTransacciones.collectLatest { transacciones ->
                transaccionAdapter.submitList(transacciones)

                val balance = transacciones.sumOf { 
                    when (it.tipo) {
                        TipoTransaccion.INGRESO.name -> it.cantidad
                        TipoTransaccion.GASTO.name -> -it.cantidad
                        else -> 0.0
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
