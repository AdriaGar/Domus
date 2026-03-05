package com.example.domus.app.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domus.data.Entity.Entity_Familia
import com.example.domus.data.database.DB_Domus
import com.example.domus.data.model.Family
import com.example.domus.data.repository.MemberInfo
import com.example.domus.data.repository.Repo_Familia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class FamilyState {
    object Loading : FamilyState()
    object NoFamily : FamilyState()
    data class PendingApproval(val familyName: String) : FamilyState()
    data class HasFamily(
        val family: Family,
        val isAdmin: Boolean,
        val isCreator: Boolean,
        val membersDetails: List<MemberInfo>,
        val pendingDetails: List<MemberInfo>
    ) : FamilyState()
    data class Error(val message: String) : FamilyState()
}

class VM_Familia(application: Application) : AndroidViewModel(application) {
    private val repository = Repo_Familia()
    private val auth = FirebaseAuth.getInstance()
    private val db = DB_Domus.getDatabase(application)
    private val familiaDao = db.familiaDao()

    private val _state = MutableStateFlow<FamilyState>(FamilyState.Loading)
    val state = _state.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        loadFamilyFromCache()
        syncWithFirebase()
    }

    private fun loadFamilyFromCache() = viewModelScope.launch {
        familiaDao.getFamilia().collect { entity ->
            if (entity != null) {
                val family = Family(
                    id = entity.id,
                    name = entity.name,
                    adminId = entity.adminId,
                    creatorId = entity.creatorId,
                    joinCode = entity.joinCode,
                    codeCreatedAt = entity.codeCreatedAt,
                    members = entity.members,
                    pendingMembers = entity.pendingMembers
                )
                // Nota: Los detalles de los miembros (fotos, nombres) 
                // se siguen cargando de Firebase por ahora para estar actualizados.
                val members = repository.getMembersDetails(family.members)
                val userId = auth.currentUser?.uid ?: ""
                val pending = if (family.adminId == userId) {
                    repository.getMembersDetails(family.pendingMembers)
                } else emptyList()

                _state.value = FamilyState.HasFamily(
                    family = family,
                    isAdmin = family.adminId == userId,
                    isCreator = family.creatorId == userId,
                    membersDetails = members,
                    pendingDetails = pending
                )
            }
        }
    }

    fun syncWithFirebase() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _isSyncing.value = true
        
        repository.getFamilyByUserId(userId).onSuccess { family ->
            if (family != null) {
                // Actualizar caché local
                val entity = Entity_Familia(
                    id = family.id,
                    name = family.name,
                    adminId = family.adminId,
                    creatorId = family.creatorId,
                    joinCode = family.joinCode,
                    codeCreatedAt = family.codeCreatedAt,
                    members = family.members,
                    pendingMembers = family.pendingMembers
                )
                familiaDao.deleteFamilia()
                familiaDao.insertFamilia(entity)
                
                // El observador de loadFamilyFromCache actualizará la UI automáticamente
            } else {
                familiaDao.deleteFamilia()
                checkPendingRequest(userId)
            }
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error de sincronización")
        }
        _isSyncing.value = false
    }

    private suspend fun checkPendingRequest(userId: String) {
        repository.getPendingFamilyRequest(userId).onSuccess { family ->
            if (family != null) {
                _state.value = FamilyState.PendingApproval(family.name)
            } else {
                _state.value = FamilyState.NoFamily
            }
        }.onFailure {
            _state.value = FamilyState.NoFamily
        }
    }

    fun createFamily(name: String) = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _state.value = FamilyState.Loading
        repository.createFamily(name, userId).onSuccess {
            syncWithFirebase()
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error al crear familia")
        }
    }

    fun joinFamily(code: String) = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _state.value = FamilyState.Loading
        repository.joinFamilyRequest(userId, code).onSuccess {
            syncWithFirebase()
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error al unirse")
        }
    }

    fun cancelJoinRequest() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        _state.value = FamilyState.Loading
        repository.cancelJoinRequest(userId).onSuccess {
            syncWithFirebase()
        }.onFailure {
            _state.value = FamilyState.Error(it.message ?: "Error al cancelar solicitud")
        }
    }

    fun leaveFamily() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily) {
            _state.value = FamilyState.Loading
            repository.leaveFamily(userId, currentState.family.id).onSuccess {
                familiaDao.deleteFamilia()
                syncWithFirebase()
            }.onFailure {
                _state.value = FamilyState.Error(it.message ?: "Error al salir de la familia")
            }
        }
    }

    fun dissolveFamily() = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            _state.value = FamilyState.Loading
            repository.dissolveFamily(currentState.family.id).onSuccess {
                familiaDao.deleteFamilia()
                syncWithFirebase()
            }.onFailure {
                _state.value = FamilyState.Error(it.message ?: "Error al disolver la familia")
            }
        }
    }

    fun transferAdmin(newAdminId: String) = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            _state.value = FamilyState.Loading
            repository.transferAdmin(currentState.family.id, newAdminId).onSuccess {
                syncWithFirebase()
            }.onFailure {
                _state.value = FamilyState.Error(it.message ?: "Error al transferir administrador")
            }
        }
    }

    fun regenerateCode() = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            repository.regenerateCode(currentState.family.id).onSuccess {
                syncWithFirebase()
            }
        }
    }

    fun acceptMember(userId: String) = viewModelScope.launch {
        val currentState = _state.value
        if (currentState is FamilyState.HasFamily && currentState.isAdmin) {
            repository.acceptMember(currentState.family.id, userId).onSuccess {
                syncWithFirebase()
            }
        }
    }
}