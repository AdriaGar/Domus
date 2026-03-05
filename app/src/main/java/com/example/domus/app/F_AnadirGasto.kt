package com.example.domus.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                        val row = binding.llParticipantesContainer.getChildAt(i) as LinearLayout
                        val checkBox = row.getChildAt(0) as CheckBox
                        checkBox.isChecked = tx.participantes.contains(checkBox.tag as String)
                    }
                }
                actualizarCalculos()
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
                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(0, 4, 0, 4)
                    }

                    val checkBox = CheckBox(requireContext()).apply {
                        text = user.nombre
                        tag = user.uid
                        isChecked = true
                        isEnabled = enModoEdicion
                        buttonTintList = ColorStateList.valueOf(getThemeColor(com.google.android.material.R.attr.colorPrimary))
                        setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnCheckedChangeListener { _, _ -> actualizarCalculos() }
                    }

                    val tvAmount = TextView(requireContext()).apply {
                        text = "0.00 €"
                        textSize = 16f
                        setTextColor(getThemeColor(com.google.android.material.R.attr.colorPrimary))
                        gravity = Gravity.END
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    row.addView(checkBox)
                    row.addView(tvAmount)
                    binding.llParticipantesContainer.addView(row)
                }
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, usuarios.map { it.nombre })
                binding.spinnerPagadoPor.setAdapter(adapter)
                binding.spinnerTransferenciaA.setAdapter(adapter)
                
                actualizarCalculos()
            }
        }
    }

    private fun setupListeners() {
        binding.cardGasto.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.GASTO) }
        binding.cardIngreso.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.INGRESO) }
        binding.cardTransferencia.setOnClickListener { if (enModoEdicion) updateSelection(TipoTransaccion.TRANSFERENCIA) }
        binding.tietFecha.setOnClickListener { if (enModoEdicion) showDatePicker() }
        binding.tilFecha.setEndIconOnClickListener { if (enModoEdicion) showDatePicker() }
        
        binding.tietImporte.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { actualizarCalculos() }
        })

        binding.btnEliminar.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Eliminar transacción?")
                .setMessage("Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    transaccionActual?.let { tx ->
                        viewModel.deleteTransaccion(tx)
                        findNavController().navigateUp()
                    }
                }
                .show()
        }
    }

    private fun actualizarCalculos() {
        val importeStr = binding.tietImporte.text.toString()
        val importeTotal = importeStr.toDoubleOrNull() ?: 0.0
        
        val seleccionados = mutableListOf<TextView>()
        for (i in 0 until binding.llParticipantesContainer.childCount) {
            val row = binding.llParticipantesContainer.getChildAt(i) as LinearLayout
            val cb = row.getChildAt(0) as CheckBox
            val tv = row.getChildAt(1) as TextView
            if (cb.isChecked) {
                seleccionados.add(tv)
            } else {
                tv.text = "0.00 €"
            }
        }

        if (seleccionados.isNotEmpty() && importeTotal > 0) {
            val cuota = importeTotal / seleccionados.size
            val cuotaStr = String.format("%.2f €", cuota)
            seleccionados.forEach { it.text = cuotaStr }
        }
    }

    private fun updateSelection(tipo: TipoTransaccion) {
        tipoSeleccionado = tipo
        updateUiForSelectedType()
        actualizarCalculos()
    }

    private fun actualizarEstadoUI() {
        val isEnabled = enModoEdicion
        binding.tietDescripcion.isEnabled = isEnabled
        binding.tietImporte.isEnabled = isEnabled
        binding.tietFecha.isEnabled = isEnabled
        binding.spinnerPagadoPor.isEnabled = isEnabled
        binding.spinnerTransferenciaA.isEnabled = isEnabled
        
        for (i in 0 until binding.llParticipantesContainer.childCount) {
            val row = binding.llParticipantesContainer.getChildAt(i) as LinearLayout
            row.getChildAt(0).isEnabled = isEnabled
        }

        binding.toolbar.menu.findItem(R.id.action_save).isVisible = isEnabled
        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = !isEnabled
        
        binding.btnEliminar.visibility = if (transaccionActual != null && isEnabled) View.VISIBLE else View.GONE
        
        updateUiForSelectedType()
    }

    private fun updateUiForSelectedType() {
        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val onPrimaryColor = getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)

        // Tarjeta Gasto
        binding.cardGasto.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.GASTO) primaryColor else surfaceColor)
        setCardContentColor(binding.cardGasto, if (tipoSeleccionado == TipoTransaccion.GASTO) onPrimaryColor else onSurfaceColor)

        // Tarjeta Ingreso
        binding.cardIngreso.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.INGRESO) primaryColor else surfaceColor)
        setCardContentColor(binding.cardIngreso, if (tipoSeleccionado == TipoTransaccion.INGRESO) onPrimaryColor else onSurfaceColor)

        // Tarjeta Transferencia
        binding.cardTransferencia.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) primaryColor else surfaceColor)
        setCardContentColor(binding.cardTransferencia, if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) onPrimaryColor else onSurfaceColor)

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

    private fun setCardContentColor(card: View, color: Int) {
        val layout = (card as ViewGroup).getChildAt(0) as ViewGroup
        val icon = layout.getChildAt(0) as android.widget.ImageView
        val text = layout.getChildAt(1) as TextView
        icon.imageTintList = ColorStateList.valueOf(color)
        text.setTextColor(color)
    }

    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
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
                val row = binding.llParticipantesContainer.getChildAt(i) as LinearLayout
                val cb = row.getChildAt(0) as CheckBox
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
                familiaId = transaccionActual?.familiaId ?: viewModel.currentFamiliaId.value,
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
