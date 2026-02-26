package com.example.domus.sesion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.domus.app.A_Applicacion
import com.example.domus.databinding.ActivitySesionBinding
import com.google.firebase.auth.FirebaseAuth

class A_Sesion : AppCompatActivity() {

    private lateinit var binding: ActivitySesionBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
    }

    public override fun onStart() {
        super.onStart()
        // Comprobar si el usuario ya ha iniciado sesión
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si hay un usuario, ir directamente a la aplicación principal
            val intent = Intent(this, A_Applicacion::class.java)
            startActivity(intent)
            finish() // Finalizar esta actividad para que el usuario no pueda volver atrás
        }
        // Si no hay usuario, la actividad continuará y mostrará los fragmentos de inicio de sesión/registro
    }
}