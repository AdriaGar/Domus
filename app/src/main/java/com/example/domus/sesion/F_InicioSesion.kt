package com.example.domus.sesion

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.domus.R
import com.example.domus.app.A_Applicacion
import com.example.domus.databinding.FragmentInicioSesionBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class F_InicioSesion : Fragment() {

    private lateinit var binding: FragmentInicioSesionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(requireContext(), "El inicio de sesión con Google ha sido cancelado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentInicioSesionBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        handlePasswordlessSignIn()
        setupListeners()

        return binding.root
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Actualizamos los datos (incluida la foto) en cada inicio de sesión
                        saveGoogleUserToFirestore(
                            user.uid,
                            user.displayName ?: "",
                            user.email ?: "",
                            user.photoUrl?.toString()
                        )
                    }
                    val intent = Intent(requireContext(), A_Applicacion::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "No se pudo iniciar sesión con Google.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveGoogleUserToFirestore(userId: String, name: String, email: String, photoUrl: String?) {
        val userDetails = mutableMapOf<String, Any>(
            "email" to email
        )
        
        // Actualizamos siempre la URL de la foto para reflejar cambios en la cuenta de Google
        if (photoUrl != null) {
            userDetails["photoUrl"] = photoUrl
        }
        
        // El nombre solo lo ponemos si no existe o si queremos forzar la actualización
        if (name.isNotEmpty()) {
            userDetails["nombre"] = name
        }

        db.collection("users").document(userId)
            .set(userDetails, SetOptions.merge())
            .addOnFailureListener { e ->
                android.util.Log.e("F_InicioSesion", "Error al actualizar usuario en Firestore", e)
            }
    }

    private fun handlePasswordlessSignIn() {
        val intent = requireActivity().intent
        val emailLink = intent.data.toString()

        if (auth.isSignInWithEmailLink(emailLink)) {
            val email = "USER_EMAIL" 
            auth.signInWithEmailLink(email, emailLink)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(requireContext(), A_Applicacion::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "Error al iniciar sesión con el enlace.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setupListeners() {
        binding.btnIniciarSesion.setOnClickListener {
            val email = binding.tietEmail.text.toString().trim()
            val password = binding.tietPassword.text.toString().trim()

            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (validateInput(email, password)) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(requireContext(), A_Applicacion::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            handleSignInError(task.exception)
                        }
                    }
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            // Cerramos sesión de Google antes para forzar la selección de cuenta y refrescar el perfil
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        binding.tvIrRegistro.setOnClickListener {
            findNavController().navigate(R.id.action_f_InicioSesion_to_f_Registro)
        }
    }

    private fun handleSignInError(exception: Exception?) {
        val errorMessage = when ((exception as? FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
            "ERROR_USER_NOT_FOUND" -> "No se encontró ninguna cuenta con este correo."
            "ERROR_WRONG_PASSWORD" -> "La contraseña es incorrecta."
            "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada."
            else -> "Correo o contraseña incorrectos."
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun validateInput(email: String, pass: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Introduce un correo válido"
            return false
        }
        if (pass.isEmpty()) {
            binding.tilPassword.error = "La contraseña no puede estar vacía"
            return false
        }
        return true
    }
}
