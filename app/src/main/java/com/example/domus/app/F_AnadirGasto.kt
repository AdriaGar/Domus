package com.example.domus.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.domus.R
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.User
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.FragmentAnadirGastoBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class F_AnadirGasto : Fragment() {

    private var _binding: FragmentAnadirGastoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_Cuentas by activityViewModels {
        CuentasViewModelFactory(requireActivity().application)
    }
    private val args: F_AnadirGastoArgs by navArgs()

    private var transaccionActual: Transaccion? = null
    private var enModoEdicion = false
    private var fechaSeleccionada: Date = Date()
    private var tipoSeleccionado = TipoTransaccion.GASTO
    private var usuarios: List<User> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnadirGastoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeUsers()

        if (args.transaccionId != null && args.transaccionId != "null") {
            enModoEdicion = false
            loadTransaccionData(args.transaccionId!!)
        } else {
            enModoEdicion = true
            setupInitialState()
        }
    }

    private fun loadTransaccionData(id: String) {
        lifecycleScope.launch {
            usuarios = viewModel.users.first { it.isNotEmpty() }
            
            transaccionActual = viewModel.getTransaccionById(id)
            transaccionActual?.let { tx ->
                binding.tietDescripcion.setText(tx.descripcion)
                binding.tietImporte.setText(tx.cantidad.toString())
                tipoSeleccionado = TipoTransaccion.valueOf(tx.tipo)
                fechaSeleccionada = tx.fecha ?: Date()
                updateFechaEnVista()
                
                val pagador = usuarios.find { it.uid == tx.usuarioId }
                binding.spinnerPagadoPor.setText(pagador?.nombre ?: "", false)

                if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) {
                    val destinoId = tx.participantes.firstOrNull()
                    val destino = usuarios.find { it.uid == destinoId }
                    binding.spinnerTransferenciaA.setText(destino?.nombre ?: "", false)
                } else {
                    for (i in 0 until binding.llParticipantesContainer.childCount) {
                        val checkBox = binding.llParticipantesContainer.getChildAt(i) as CheckBox
                        checkBox.isChecked = tx.participantes.contains(checkBox.tag as String)
                    }
                }
                actualizarEstadoUI()
            } ?: run {
                Toast.makeText(requireContext(), "Error al cargar la transacción", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    guardarTransaccion()
                    true
                }
                R.id.action_edit -> {
                    enModoEdicion = true
                    actualizarEstadoUI()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupInitialState() {
        updateFechaEnVista()
        actualizarEstadoUI()
    }

    private fun observeUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collectLatest { userList ->
                usuarios = userList
                
                binding.llParticipantesContainer.removeAllViews()
                usuarios.forEach { user ->
                    val checkBox = CheckBox(requireContext()).apply {
                        text = user.nombre
                        tag = user.uid
                        isChecked = true
                        isEnabled = enModoEdicion
                    }
                    binding.llParticipantesContainer.addView(checkBox)
                }
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, usuarios.map { it.nombre })
                binding.spinnerPagadoPor.setAdapter(adapter)
                binding.spinnerTransferenciaA.setAdapter(adapter)
            }
        }
    }

    private fun setupListeners() {
        binding.cardGasto.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.GASTO) }
        binding.cardIngreso.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.INGRESO) }
        binding.cardTransferencia.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.TRANSFERENCIA) }
        binding.tietFecha.setOnClickListener { if (enModoEdicion) showDatePicker() }
        binding.tilFecha.setEndIconOnClickListener { if (enModoEdicion) showDatePicker() }
    }

    private fun updateSelection(tipo: TipoTransaccion) {
        tipoSeleccionado = tipo
        updateUiForSelectedType()
    }

    private fun actualizarEstadoUI() {
        val isEnabled = enModoEdicion
        binding.tietDescripcion.isEnabled = isEnabled
        binding.tietImporte.isEnabled = isEnabled
        binding.tietFecha.isEnabled = isEnabled
        binding.spinnerPagadoPor.isEnabled = isEnabled
        binding.spinnerTransferenciaA.isEnabled = isEnabled
        
        for (i in 0 until binding.llParticipantesContainer.childCount) {
            binding.llParticipantesContainer.getChildAt(i).isEnabled = isEnabled
        }

        binding.toolbar.menu.findItem(R.id.action_save).isVisible = isEnabled
        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = !isEnabled
        
        updateUiForSelectedType()
    }

    private fun updateUiForSelectedType() {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.card_selected_background)
        val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        binding.cardGasto.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.GASTO) selectedColor else defaultColor)
        binding.cardIngreso.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.INGRESO) selectedColor else defaultColor)
        binding.cardTransferencia.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) selectedColor else defaultColor)

        when (tipoSeleccionado) {
            TipoTransaccion.GASTO, TipoTransaccion.INGRESO -> {
                binding.tilPagadoPor.visibility = View.VISIBLE
                binding.tvParticipantesLabel.visibility = View.VISIBLE
                binding.llParticipantesContainer.visibility = View.VISIBLE
                binding.tilTransferencia.visibility = View.GONE
                binding.tvParticipantesLabel.text = if(tipoSeleccionado == TipoTransaccion.GASTO) "Dividido entre" else "Ingreso para"
            }
            TipoTransaccion.TRANSFERENCIA -> {
                binding.tilPagadoPor.visibility = View.VISIBLE
                binding.tvParticipantesLabel.visibility = View.GONE
                binding.llParticipantesContainer.visibility = View.GONE
                binding.tilTransferencia.visibility = View.VISIBLE
            }
        }
    }

    private fun guardarTransaccion() {
        val descripcion = binding.tietDescripcion.text.toString().trim()
        val importeStr = binding.tietImporte.text.toString().trim()
        
        if (descripcion.isEmpty() || importeStr.isEmpty()) {
            Toast.makeText(requireContext(), "Rellena la descripción y el importe", Toast.LENGTH_SHORT).show()
            return
        }

        val pagadorNombre = binding.spinnerPagadoPor.text.toString()
        val pagador = usuarios.find { it.nombre == pagadorNombre }
        if (pagador == null) {
            Toast.makeText(requireContext(), "Selecciona quién realiza el pago", Toast.LENGTH_SHORT).show()
            return
        }

        val participantes = mutableListOf<String>()
        if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) {
            val destNombre = binding.spinnerTransferenciaA.text.toString()
            val dest = usuarios.find { it.nombre == destNombre }
            if (dest != null) {
                participantes.add(dest.uid)
            } else {
                Toast.makeText(requireContext(), "Selecciona un destinatario", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            for (i in 0 until binding.llParticipantesContainer.childCount) {
                val cb = binding.llParticipantesContainer.getChildAt(i) as CheckBox
                if (cb.isChecked) {
                    participantes.add(cb.tag as String)
                }
            }
            if (participantes.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos un participante", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            val transaccion = Transaccion(
                id = transaccionActual?.id ?: "",
                descripcion = descripcion,
                cantidad = importeStr.toDouble(),
                tipo = tipoSeleccionado.name,
                usuarioId = pagador.uid,
                usuarioNombre = pagador.nombre,
                participantes = participantes,
                fecha = fechaSeleccionada
            )

            if (transaccionActual == null) {
                viewModel.addTransaccion(transaccion)
            } else {
                viewModel.updateTransaccion(transaccion)
            }
            findNavController().navigateUp()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFechaEnVista() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tietFecha.setText(formato.format(fechaSeleccionada))
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(fechaSeleccionada.time)
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val timeZone = TimeZone.getDefault()
            val offset = timeZone.getOffset(Date().time) * -1
            fechaSeleccionada = Date(selection + offset)
            updateFechaEnVista()
        }
        datePicker.show(parentFragmentManager, "DatePicker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
