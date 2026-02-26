package com.example.domus.sesion

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domus.data.Entity.Entity_Usuario
import com.example.domus.data.repository.Repo_Usuario
import kotlinx.coroutines.launch

class VM_InicioSesion : ViewModel() {

    private val repo_Usuario = Repo_Usuario()


    /**
     * Valida los datos de inicio de sesión.
     * Retorna Result.success(true) si son válidos o Result.failure con la excepción si no.
     */
    fun validarDatos(email: String, password: String): Result<Boolean> {
        return when {
            email.isEmpty() -> Result.failure(Exception("El correo electrónico es obligatorio"))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Result.failure(Exception("El formato del correo no es válido"))
            password.isEmpty() -> Result.failure(Exception("La contraseña es obligatoria"))
            password.length < 8 -> Result.failure(Exception("La contraseña debe tener al menos 8 caracteres"))
            else -> Result.success(true)
        }
    }

    fun subirUsuario(usuario: Entity_Usuario) {
        viewModelScope.launch{
            repo_Usuario.sendNewUser(usuario)
        }
    }
}