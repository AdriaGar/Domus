package com.example.domus.app.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domus.data.model.Family
import com.example.domus.data.repository.MemberInfo
import com.example.domus.data.repository.Repo_Familia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FamilyState {
    object Loading : FamilyState()
    object NoFamily : FamilyState()
    data class HasFamily(
        val family: Family,
        val isAdmin: Boolean,
        val membersDetails: List<MemberInfo>,
        val pendingDetails: List<MemberInfo>
    ) : FamilyState()
    data class Error(val message: String) : FamilyState()
}

class VM_Familia : ViewModel() {
    private val repository = Repo_Familia()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<FamilyState>(FamilyState.Loading)
    val state = _state.asStateFlow()

    init {
        loadFamily()
    }

    fun loadFamily() = viewModelScope.launch {
        _state.value = FamilyState.Loading
        val userId = auth.currentUser?.uid ?: return@launch
        repository.getFamilyByUserId(userId).onSuccess { family ->
            if (family != null) {
                val members = repository.getMembersDetails(family.members)
                val pending = if (family.adminId == userId) {
                    repository.getMembersDetails(family.pendingMembers)
                } else emptyList()
                
                _state.value = FamilyState.HasFamily(family, family.adminId == userId, members, pending)
            } else {
                _state.value = FamilyState.NoFamily
            }
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error desconocido")
        }
    }

    fun createFamily(name: String) = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _state.value = FamilyState.Loading
        repository.createFamily(name, userId).onSuccess {
            loadFamily()
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error al crear familia")
        }
    }

    fun joinFamily(code: String) = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _state.value = FamilyState.Loading
        repository.joinFamilyRequest(userId, code).onSuccess {
            loadFamily()
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error al unirse")
        }
    }

    fun regenerateCode() = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            repository.regenerateCode(currentState.family.id).onSuccess {
                loadFamily()
            }
        }
    }

    fun acceptMember(userId: String) = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            repository.acceptMember(currentState.family.id, userId).onSuccess {
                loadFamily()
            }
        }
    }
}