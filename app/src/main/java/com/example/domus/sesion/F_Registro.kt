package com.example.domus.sesion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.domus.R
import com.example.domus.databinding.FragmentRegistroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthException

class F_Registro : Fragment() {

    private lateinit var binding: FragmentRegistroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRegistroBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        setupListeners()

        return binding.root
    }

    private fun setupListeners() {
        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.tietNombre.text.toString().trim()
            val apellidos = binding.tietApellidos.text.toString().trim()
            val email = binding.tietEmailReg.text.toString().trim()
            val password = binding.tietPasswordReg.text.toString().trim()
            val confirmPassword = binding.tietConfirmPassword.text.toString().trim()

            clearErrors()

            if (validateInput(nombre, apellidos, email, password, confirmPassword)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Cuenta creada con éxito.", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_f_Registro_to_f_InicioSesion)
                        } else {
                            handleRegistrationError(task.exception)
                        }
                    }
            }
        }

        binding.tvIrLogin.setOnClickListener {
            findNavController().navigate(R.id.action_f_Registro_to_f_InicioSesion)
        }

        // DEBERAS AÑADIR ESTE BOTON EN TU LAYOUT
        // binding.btnEnviarEnlace.setOnClickListener {
        //     val email = binding.tietEmailReg.text.toString().trim()
        //     if (email.isNotEmpty()) {
        //         sendPasswordlessSignInLink(email)
        //     } else {
        //         binding.tilEmailReg.error = "Introduce tu correo electrónico"
        //     }
        // }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when ((exception as? FirebaseAuthException)?.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "La dirección de correo ya está registrada."
            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
            "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil, debe tener al menos 6 caracteres."
            else -> "No se pudo completar el registro. Inténtalo de nuevo."
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun sendPasswordlessSignInLink(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://domus-f991c.firebaseapp.com") // URL de tu app para continuar el flujo
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                requireContext().packageName,
                true, /* installIfNotAvailable */
                null /* minimumVersion */)
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Enlace de inicio de sesión enviado a tu correo.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al enviar el enlace: ${'$'}{task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInput(nombre: String, apellidos: String, email: String, pass: String, confirmPass: String): Boolean {
        if (nombre.isEmpty()) {
            binding.tilNombre.error = "El nombre no puede estar vacío"
            return false
        }
        if (apellidos.isEmpty()) {
            binding.tilApellidos.error = "Los apellidos no pueden estar vacíos"
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmailReg.error = "Introduce un correo válido"
            return false
        }
        if (pass.length < 6) {
            binding.tilPasswordReg.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        if (pass != confirmPass) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }
        return true
    }

    private fun clearErrors() {
        binding.tilNombre.error = null
        binding.tilApellidos.error = null
        binding.tilEmailReg.error = null
        binding.tilPasswordReg.error = null
        binding.tilConfirmPassword.error = null
    }
}