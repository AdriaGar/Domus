package com.example.domus.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.R
import com.example.domus.app.viewModel.CuentasViewModelFactory
import com.example.domus.app.viewModel.User
import com.example.domus.app.viewModel.VM_Cuentas
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.FragmentCuentasBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs
import kotlin.math.min

class F_Cuentas : Fragment() {

    private var _binding: FragmentCuentasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_Cuentas by activityViewModels {
        CuentasViewModelFactory(requireActivity().application)
    }

    private lateinit var transaccionAdapter: Adapt_Transaccion
    private var isExpanded = false

    private fun formatName(fullName: String): String {
        val parts = fullName.trim().split("\\s+".toRegex())
        return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else fullName
    }

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
        setupExpansion()
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
            findNavController().navigate(R.id.action_f_Cuentas_to_f_AnadirGasto)
        }
    }

    private fun setupExpansion() {
        binding.llBalanceHeader.setOnClickListener {
            isExpanded = !isExpanded
            binding.llBalancesExpansion.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.allTransacciones, viewModel.users) { transacciones, users ->
                Pair(transacciones, users)
            }.collect { (transacciones, users) ->
                // Actualizamos los usuarios en el adaptador antes de enviar la lista de transacciones
                transaccionAdapter.setUsers(users)
                transaccionAdapter.submitList(transacciones)
                
                val totalGasto = transacciones.filter { it.tipo == TipoTransaccion.GASTO.name }
                                             .sumOf { it.cantidad }
                
                binding.tvBalance.text = String.format("%.2f €", totalGasto)

                if (users.isNotEmpty()) {
                    calculateAndShowBalances(transacciones, users)
                }
            }
        }
    }

    private fun calculateAndShowBalances(transacciones: List<Transaccion>, users: List<User>) {
        val balances = users.associate { it.uid to 0.0 }.toMutableMap()
        val allUserIds = users.map { it.uid }

        transacciones.forEach { tx ->
            val amount = tx.cantidad
            if (amount <= 0) return@forEach

            when (tx.tipo) {
                TipoTransaccion.GASTO.name -> {
                    balances[tx.usuarioId] = (balances[tx.usuarioId] ?: 0.0) + amount
                    val participants = if (tx.participantes.isEmpty()) allUserIds else tx.participantes
                    val share = amount / participants.size
                    participants.forEach { partId ->
                        balances[partId] = (balances[partId] ?: 0.0) - share
                    }
                }
                TipoTransaccion.INGRESO.name -> {
                    balances[tx.usuarioId] = (balances[tx.usuarioId] ?: 0.0) - amount
                    val beneficiaries = if (tx.participantes.isEmpty()) allUserIds else tx.participantes
                    val share = amount / beneficiaries.size
                    beneficiaries.forEach { partId ->
                        balances[partId] = (balances[partId] ?: 0.0) + share
                    }
                }
                TipoTransaccion.TRANSFERENCIA.name -> {
                    balances[tx.usuarioId] = (balances[tx.usuarioId] ?: 0.0) + amount
                    tx.participantes.firstOrNull()?.let { destId ->
                        balances[destId] = (balances[destId] ?: 0.0) - amount
                    }
                }
            }
        }

        renderMemberBalances(balances, users)
        renderDebts(balances, users)
    }

    private fun renderMemberBalances(balances: Map<String, Double>, users: List<User>) {
        binding.llMemberBalancesContainer.removeAllViews()
        users.forEach { user ->
            val balance = balances[user.uid] ?: 0.0
            val row = createBalanceRow(formatName(user.nombre), balance)
            binding.llMemberBalancesContainer.addView(row)
        }
    }

    data class Debt(val debtorId: String, val creditorId: String, val amount: Double)

    private fun renderDebts(balances: Map<String, Double>, users: List<User>) {
        binding.llDebtsContainer.removeAllViews()
        
        val debtors = balances.filter { it.value < -0.01 }.mapValues { abs(it.value) }.toMutableMap()
        val creditors = balances.filter { it.value > 0.01 }.toMutableMap()

        val allDebts = mutableListOf<Debt>()
        val debtorKeys = debtors.keys.toMutableList()
        val creditorKeys = creditors.keys.toMutableList()
        
        while (debtorKeys.isNotEmpty() && creditorKeys.isNotEmpty()) {
            val debtorId = debtorKeys.first()
            val creditorId = creditorKeys.first()
            val dAmount = debtors[debtorId] ?: 0.0
            val cAmount = creditors[creditorId] ?: 0.0
            val settleAmount = min(dAmount, cAmount)
            
            if (settleAmount > 0.01) {
                allDebts.add(Debt(debtorId, creditorId, settleAmount))
            }

            debtors[debtorId] = dAmount - settleAmount
            creditors[creditorId] = cAmount - settleAmount
            
            if ((debtors[debtorId] ?: 0.0) < 0.01) debtorKeys.removeAt(0)
            if ((creditors[creditorId] ?: 0.0) < 0.01) creditorKeys.removeAt(0)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val myDebts = allDebts.filter { it.debtorId == currentUserId }
        val debtsToMe = allDebts.filter { it.creditorId == currentUserId }

        if (myDebts.isEmpty() && debtsToMe.isEmpty()) {
            val tvEmpty = TextView(requireContext()).apply {
                text = "Cuentas al día"
                setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                textSize = 13f
                setPadding(0, 16, 0, 16)
                gravity = Gravity.CENTER
            }
            binding.llDebtsContainer.addView(tvEmpty)
            return
        }

        myDebts.forEach { debt ->
            val creditor = users.find { it.uid == debt.creditorId }
            val creditorName = if (creditor != null) formatName(creditor.nombre) else "Alguien"
            binding.llDebtsContainer.addView(createDebtRow(creditorName, debt.amount, true) {
                pagarDeuda(debt, users)
            })
        }

        debtsToMe.forEach { debt ->
            val debtor = users.find { it.uid == debt.debtorId }
            val debtorName = if (debtor != null) formatName(debtor.nombre) else "Alguien"
            binding.llDebtsContainer.addView(createDebtRow(debtorName, debt.amount, false) {})
        }
    }

    private fun pagarDeuda(debt: Debt, users: List<User>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUser = users.find { it.uid == currentUserId }
        val currentUserName = if (currentUser != null) formatName(currentUser.nombre) else "Yo"
        val creditor = users.find { it.uid == debt.creditorId }
        val creditorName = if (creditor != null) formatName(creditor.nombre) else "Alguien"
        
        val transaccion = Transaccion(
            descripcion = "Abono de deuda a $creditorName",
            cantidad = debt.amount,
            tipo = TipoTransaccion.TRANSFERENCIA.name,
            usuarioId = currentUserId,
            usuarioNombre = currentUser?.nombre ?: "Yo",
            participantes = listOf(debt.creditorId),
            fecha = Date(),
            familiaId = viewModel.currentFamiliaId.value
        )
        
        viewModel.addTransaccion(transaccion)
        Toast.makeText(requireContext(), "Abonando ${String.format("%.2f €", debt.amount)}", Toast.LENGTH_SHORT).show()
    }

    private fun createDebtRow(otherName: String, amount: Double, isPayable: Boolean, onPay: () -> Unit): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val tvText = TextView(requireContext()).apply {
            text = if (isPayable) "Debes a $otherName: " else "Te debe $otherName: "
            setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            textSize = 14f
        }

        val tvAmount = TextView(requireContext()).apply {
            text = String.format("%.2f €", amount)
            setTextColor(if (isPayable) Color.parseColor("#EF5350") else Color.parseColor("#66BB6A"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        container.addView(tvText)
        container.addView(tvAmount)

        if (isPayable) {
            val btnPay = MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = "Abonar"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.accent))
                textSize = 12f
                setOnClickListener { onPay() }
            }
            container.addView(btnPay)
        }

        return container
    }

    private fun createBalanceRow(name: String, amount: Double): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }

        val tvName = TextView(requireContext()).apply {
            text = name
            setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvAmount = TextView(requireContext()).apply {
            text = String.format("%.2f €", amount)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
            
            if (amount > 0.01) {
                text = "+${String.format("%.2f €", amount)}"
                setTextColor(Color.parseColor("#66BB6A"))
            } else if (amount < -0.01) {
                setTextColor(Color.parseColor("#EF5350"))
            } else {
                setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            }
        }

        container.addView(tvName)
        container.addView(tvAmount)
        return container
    }

    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
