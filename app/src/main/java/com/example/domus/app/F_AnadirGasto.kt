package com.example.domus.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.domus.R
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.FragmentAnadirGastoBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class F_AnadirGasto : DialogFragment() {

    private var _binding: FragmentAnadirGastoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_Cuentas by activityViewModels {
        CuentasViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnadirGastoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialState()
        setupListeners()
        observeUsers()
    }

    private fun setupInitialState() {
        binding.chipGasto.isChecked = true
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collectLatest { users ->
                binding.llParticipantesContainer.removeAllViews() // Limpiar vistas anteriores
                users.forEach { user ->
                    val checkBox = CheckBox(requireContext()).apply {
                        text = user.nombre
                        tag = user.uid // Guardamos el UID en el tag para recuperarlo luego
                        isChecked = true // Por defecto, todos participan
                    }
                    binding.llParticipantesContainer.addView(checkBox)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancelar.setOnClickListener { dismiss() }

        binding.btnGuardar.setOnClickListener {
            val descripcion = binding.tietDescripcion.text.toString().trim()
            val cantidadStr = binding.tietCantidad.text.toString().trim()

            val tipoSeleccionado = when (binding.chipGroupTipo.checkedChipId) {
                R.id.chip_ingreso -> TipoTransaccion.INGRESO
                R.id.chip_transferencia -> TipoTransaccion.TRANSFERENCIA
                else -> TipoTransaccion.GASTO
            }

            val participantesSeleccionados = mutableListOf<String>()
            for (i in 0 until binding.llParticipantesContainer.childCount) {
                val checkBox = binding.llParticipantesContainer.getChildAt(i) as CheckBox
                if (checkBox.isChecked) {
                    participantesSeleccionados.add(checkBox.tag as String)
                }
            }

            if (descripcion.isNotEmpty() && cantidadStr.isNotEmpty() && participantesSeleccionados.isNotEmpty()) {
                try {
                    val cantidad = cantidadStr.toDouble()
                    val nuevaTransaccion = Transaccion(
                        descripcion = descripcion,
                        cantidad = cantidad,
                        tipo = tipoSeleccionado.name,
                        participantes = participantesSeleccionados
                    )
                    viewModel.addTransaccion(nuevaTransaccion)
                    dismiss()
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "La cantidad no es un número válido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Rellena todos los campos y selecciona al menos un participante", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
