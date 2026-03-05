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
import com.example.domus.databinding.FragmentGestionarFamiliaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class F_GestionarFamilia : Fragment() {

    private var _binding: FragmentGestionarFamiliaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VM_Familia by viewModels()

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
            val code = binding.etFamilyCode.text.toString().trim().uppercase()
            if (code.isNotEmpty()) {
                viewModel.joinFamily(code)
            } else {
                binding.tilFamilyCode.error = "Introduce un código"
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
            }
            is FamilyState.HasFamily -> {
                binding.cardNoFamily.isVisible = false
                binding.groupHasFamily.isVisible = true
                
                binding.tvFamilyName.text = state.family.name
                binding.tvJoinCode.text = state.family.joinCode
                
                // Formatear y mostrar la hora de expiración
                val expirationTime = state.family.codeCreatedAt + (15 * 60 * 1000)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.tvCodeTimer.text = "Válido hasta: ${sdf.format(Date(expirationTime))}"

                // Solo el admin puede regenerar el código
                binding.btnRegenerateCode.isVisible = state.isAdmin
                
                setupMembersList(state.family.members)
                
                if (state.isAdmin) {
                    binding.tvPendingLabel.isVisible = state.family.pendingMembers.isNotEmpty()
                    binding.rvPendingMembers.isVisible = state.family.pendingMembers.isNotEmpty()
                    setupPendingMembersList(state.family.pendingMembers)
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

    private fun setupMembersList(members: List<String>) {
        binding.rvMembers.layoutManager = LinearLayoutManager(context)
        // Adaptador pendiente de implementar con datos reales de Firebase
    }

    private fun setupPendingMembersList(pending: List<String>) {
        binding.rvPendingMembers.layoutManager = LinearLayoutManager(context)
        // Adaptador pendiente de implementar con opción de aceptar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}