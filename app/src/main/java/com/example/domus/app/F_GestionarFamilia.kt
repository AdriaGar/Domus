package com.example.domus.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.app.viewModel.FamilyState
import com.example.domus.app.viewModel.VM_Familia
import com.example.domus.data.repository.MemberInfo
import com.example.domus.databinding.FragmentGestionarFamiliaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class F_GestionarFamilia : Fragment() {

    private var _binding: FragmentGestionarFamiliaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VM_Familia by viewModels()

    private var membersAdapter: Adapt_MiembrosFamilia? = null
    private var pendingAdapter: Adapt_MiembrosFamilia? = null
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionarFamiliaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnJoinFamily.setOnClickListener {
            val currentState = viewModel.state.value
            if (currentState is FamilyState.PendingApproval) {
                viewModel.cancelJoinRequest()
            } else {
                val code = binding.etFamilyCode.text.toString().trim().uppercase()
                if (code.isNotEmpty()) {
                    viewModel.joinFamily(code)
                } else {
                    binding.tilFamilyCode.error = "Introduce un código"
                }
            }
        }

        binding.btnCreateFamily.setOnClickListener {
            showCreateFamilyDialog()
        }

        binding.btnShareCode.setOnClickListener {
            val state = viewModel.state.value
            if (state is FamilyState.HasFamily) {
                shareFamilyCode(state.family.joinCode)
            }
        }

        binding.btnRegenerateCode.setOnClickListener {
            viewModel.regenerateCode()
        }

        binding.btnFamilyAction.setOnClickListener {
            val state = viewModel.state.value
            if (state is FamilyState.HasFamily) {
                if (state.isAdmin) {
                    showDissolveConfirmation()
                } else {
                    showLeaveConfirmation()
                }
            }
        }
    }

    private fun showDissolveConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Disolver Familia")
            .setMessage("¿Estás seguro de que quieres disolver la familia? Todos los miembros serán expulsados y el grupo dejará de existir.")
            .setPositiveButton("Disolver") { _, _ -> viewModel.dissolveFamily() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLeaveConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Salir de la Familia")
            .setMessage("¿Estás seguro de que quieres salir de la familia?")
            .setPositiveButton("Salir") { _, _ -> viewModel.leaveFamily() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareFamilyCode(code: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Únete a mi familia en Domus")
            val shareMessage = "¡Hola! Únete a mi familia en la app Domus usando este código: $code\n\nEste código expira en 15 minutos."
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir código con..."))
    }

    private fun showCreateFamilyDialog() {
        val input = TextInputEditText(requireContext())
        input.hint = "Nombre de la familia"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Crear Familia")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createFamily(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: FamilyState) {
        binding.progressBar.isVisible = state is FamilyState.Loading
        
        when (state) {
            is FamilyState.NoFamily -> {
                binding.cardNoFamily.isVisible = true
                binding.groupHasFamily.isVisible = false
                binding.tvPendingLabel.isVisible = false
                binding.rvPendingMembers.isVisible = false
                
                // Reset UI NoFamily
                binding.tvNoFamilyTitle.text = "No perteneces a ninguna familia"
                binding.tilFamilyCode.isVisible = true
                binding.btnJoinFamily.text = "Unirse a Familia"
                binding.tvOrCreate.isVisible = true
                binding.btnCreateFamily.isVisible = true
            }
            is FamilyState.PendingApproval -> {
                binding.cardNoFamily.isVisible = true
                binding.groupHasFamily.isVisible = false
                binding.tvPendingLabel.isVisible = false
                binding.rvPendingMembers.isVisible = false

                // Update UI for Pending
                binding.tvNoFamilyTitle.text = "Solicitud de familia pendiente"
                binding.tilFamilyCode.isVisible = false
                binding.btnJoinFamily.text = "Cancelar solicitud"
                binding.tvOrCreate.isVisible = false
                binding.btnCreateFamily.isVisible = false
            }
            is FamilyState.HasFamily -> {
                binding.cardNoFamily.isVisible = false
                binding.groupHasFamily.isVisible = true
                
                // El nombre lo ven todos
                binding.tvFamilyName.text = state.family.name

                // Visibilidad exclusiva para el administrador
                binding.tvJoinCodeLabel.isVisible = state.isAdmin
                binding.tvJoinCode.isVisible = state.isAdmin
                binding.tvCodeTimer.isVisible = state.isAdmin && state.family.codeCreatedAt > 0L
                binding.btnShareCode.isVisible = state.isAdmin
                binding.btnRegenerateCode.isVisible = state.isAdmin

                // Configurar botón de acción (Salir/Disolver)
                binding.btnFamilyAction.text = if (state.isAdmin) "Disolver Familia" else "Salir de la Familia"
                binding.btnFamilyAction.isVisible = true

                if (state.isAdmin) {
                    binding.tvJoinCode.text = if (state.family.joinCode.isEmpty()) "Sin código" else state.family.joinCode
                    binding.btnShareCode.isEnabled = state.family.joinCode.isNotEmpty()
                    
                    if (state.family.codeCreatedAt > 0L) {
                        val expirationTime = state.family.codeCreatedAt + (15 * 60 * 1000)
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        binding.tvCodeTimer.text = "Válido hasta: ${sdf.format(Date(expirationTime))}"
                    }
                }
                
                setupMembersList(state.membersDetails, state.family.adminId, state.family.creatorId, state.isAdmin)
                
                if (state.isAdmin) {
                    binding.tvPendingLabel.isVisible = state.pendingDetails.isNotEmpty()
                    binding.rvPendingMembers.isVisible = state.pendingDetails.isNotEmpty()
                    setupPendingMembersList(state.pendingDetails, state.family.adminId, state.family.creatorId)
                } else {
                    binding.tvPendingLabel.isVisible = false
                    binding.rvPendingMembers.isVisible = false
                }
            }
            is FamilyState.Error -> {
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun setupMembersList(members: List<MemberInfo>, adminId: String, creatorId: String, isAdmin: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: ""
        if (membersAdapter == null) {
            membersAdapter = Adapt_MiembrosFamilia(
                members = members,
                adminId = adminId,
                creatorId = creatorId,
                isCurrentUserAdmin = isAdmin,
                currentUserId = currentUserId,
                onTransferAdminClick = { newAdminId -> viewModel.transferAdmin(newAdminId) }
            )
            binding.rvMembers.layoutManager = LinearLayoutManager(context)
            binding.rvMembers.adapter = membersAdapter
        } else {
            membersAdapter = Adapt_MiembrosFamilia(
                members = members,
                adminId = adminId,
                creatorId = creatorId,
                isCurrentUserAdmin = isAdmin,
                currentUserId = currentUserId,
                onTransferAdminClick = { newAdminId -> viewModel.transferAdmin(newAdminId) }
            )
            binding.rvMembers.adapter = membersAdapter
        }
    }

    private fun setupPendingMembersList(pending: List<MemberInfo>, adminId: String, creatorId: String) {
        if (pendingAdapter == null) {
            pendingAdapter = Adapt_MiembrosFamilia(
                members = pending,
                adminId = adminId,
                creatorId = creatorId,
                onAcceptClick = { userId -> viewModel.acceptMember(userId) }
            )
            binding.rvPendingMembers.layoutManager = LinearLayoutManager(context)
            binding.rvPendingMembers.adapter = pendingAdapter
        } else {
            pendingAdapter?.updateData(pending)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}