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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.domus.R
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.User
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.FragmentAnadirGastoBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.hdodenhof.circleimageview.CircleImageView
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
    private val seleccionadosIds = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnadirGastoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeUsers()

        val txId = if (args.transaccionId != null && args.transaccionId != "null") args.transaccionId else null
        if (txId != null) {
            enModoEdicion = false
            loadTransaccionData(txId)
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

                seleccionadosIds.clear()
                seleccionadosIds.addAll(tx.participantes)
                
                if (tipoSeleccionado == TipoTransaccion.TRANSFERENCIA) {
                    val destinoId = tx.participantes.firstOrNull()
                    val destino = usuarios.find { it.uid == destinoId }
                    binding.spinnerTransferenciaA.setText(destino?.nombre ?: "", false)
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
                if (seleccionadosIds.isEmpty()) {
                    seleccionadosIds.addAll(usuarios.map { it.uid })
                }
                renderUserSelectionList()
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, usuarios.map { it.nombre })
                binding.spinnerPagadoPor.setAdapter(adapter)
                binding.spinnerTransferenciaA.setAdapter(adapter)
                
                actualizarCalculos()
            }
        }
    }

    private fun renderUserSelectionList() {
        binding.llParticipantesContainer.removeAllViews()
        usuarios.forEach { user ->
            val userRow = LayoutInflater.from(requireContext()).inflate(R.layout.item_user_selection, binding.llParticipantesContainer, false)
            val card = userRow.findViewById<MaterialCardView>(R.id.card_user)
            val ivPhoto = userRow.findViewById<CircleImageView>(R.id.iv_user_photo)
            val tvName = userRow.findViewById<TextView>(R.id.tv_user_name)
            val tvAmount = userRow.findViewById<TextView>(R.id.tv_user_amount)

            tvName.text = user.nombre
            
            // Usamos signature con lastUpdated para forzar el refresco de la foto
            Glide.with(this)
                .load(user.photoUrl)
                .signature(ObjectKey(user.lastUpdated))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_user_default)
                .into(ivPhoto)

            updateRowVisualState(card, tvAmount, user.uid)

            userRow.setOnClickListener {
                if (!enModoEdicion) return@setOnClickListener
                if (seleccionadosIds.contains(user.uid)) {
                    seleccionadosIds.remove(user.uid)
                } else {
                    seleccionadosIds.add(user.uid)
                }
                updateRowVisualState(card, tvAmount, user.uid)
                actualizarCalculos()
            }

            userRow.tag = user.uid
            binding.llParticipantesContainer.addView(userRow)
        }
    }

    private fun updateRowVisualState(card: MaterialCardView, tvAmount: TextView, userId: String) {
        val isSelected = seleccionadosIds.contains(userId)
        val primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary)
        
        card.strokeWidth = if (isSelected) (2 * resources.displayMetrics.density).toInt() else 0
        card.strokeColor = primaryColor
        card.alpha = if (isSelected) 1.0f else 0.6f
        tvAmount.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
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
        
        val count = seleccionadosIds.size
        val cuota = if (count > 0) importeTotal / count else 0.0
        val cuotaStr = String.format("%.2f €", cuota)

        for (i in 0 until binding.llParticipantesContainer.childCount) {
            val view = binding.llParticipantesContainer.getChildAt(i)
            val userId = view.tag as String
            val tvAmount = view.findViewById<TextView>(R.id.tv_user_amount)
            if (seleccionadosIds.contains(userId)) {
                tvAmount.text = cuotaStr
            } else {
                tvAmount.text = "0.00 €"
            }
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
        
        renderUserSelectionList() 

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

        binding.cardGasto.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.GASTO) primaryColor else surfaceColor)
        setCardContentColor(binding.cardGasto, if (tipoSeleccionado == TipoTransaccion.GASTO) onPrimaryColor else onSurfaceColor)

        binding.cardIngreso.setCardBackgroundColor(if (tipoSeleccionado == TipoTransaccion.INGRESO) primaryColor else surfaceColor)
        setCardContentColor(binding.cardIngreso, if (tipoSeleccionado == TipoTransaccion.INGRESO) onPrimaryColor else onSurfaceColor)

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
            participantes.addAll(seleccionadosIds)
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
                viewModel.addTransaccion(transaccion, transaccion.familiaId)
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
