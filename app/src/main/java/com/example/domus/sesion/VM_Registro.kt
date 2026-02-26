package com.example.domus.sesion

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domus.data.Entity.Entity_Usuario
import com.example.domus.data.repository.Repo_Usuario
import kotlinx.coroutines.launch

class VM_Registro : ViewModel() {

    private val repo_Usuario = Repo_Usuario()

    private val _registroResult = MutableLiveData<Result<Boolean>>()
    val registroResult: LiveData<Result<Boolean>> = _registroResult

    fun validarDatos(usuario: Entity_Usuario, confirmPass: String): Result<Boolean> {
        return when {
            usuario.nombre.isEmpty() -> Result.failure(Exception("El nombre es obligatorio"))
            usuario.nombre.length < 3 -> Result.failure(Exception("El nombre es demasiado corto"))
            usuario.apellidos.isEmpty() -> Result.failure(Exception("Los apellidos son obligatorios"))
            usuario.apellidos.length < 3 -> Result.failure(Exception("Los apellidos son demasiado cortos"))
            usuario.email.isEmpty() -> Result.failure(Exception("El correo electrónico es obligatorio"))
            !Patterns.EMAIL_ADDRESS.matcher(usuario.email).matches() -> Result.failure(Exception("El formato del correo no es válido"))
            usuario.password.isEmpty() -> Result.failure(Exception("La contraseña es obligatoria"))
            usuario.password.length < 8 -> Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            usuario.password != confirmPass -> Result.failure(Exception("Las contraseñas no coinciden"))
            else -> Result.success(true)
        }
    }

    fun subirUsuario(usuario: Entity_Usuario) {
        viewModelScope.launch {
            val result = repo_Usuario.sendNewUser(usuario)
            _registroResult.postValue(result)
        }
    }
}