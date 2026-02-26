package com.example.domus.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.domus.R
import com.example.domus.databinding.ActivityApplicacionBinding
import com.example.domus.sesion.A_Sesion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class A_Applicacion : AppCompatActivity() {

    private lateinit var binding: ActivityApplicacionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configurar Google Sign-In para poder cerrar sesión
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setSupportActionBar(binding.topAppBar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)

        val profileMenuItem = menu?.findItem(R.id.menu_user_profile)
        val actionView = profileMenuItem?.actionView
        val profileImageView = actionView?.findViewById<CircleImageView>(R.id.iv_profile_image)

        val user = auth.currentUser
        val photoUrl = user?.photoUrl

        if (profileImageView != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_user_default)
                .error(R.drawable.ic_user_default)
                .into(profileImageView)
        }

        actionView?.setOnClickListener {
            onOptionsItemSelected(profileMenuItem!!)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_user_profile -> {
                showSignOutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar la sesión?")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                signOut()
                dialog.dismiss()
            }
            .show()
    }

    private fun signOut() {
        // Cerrar sesión de Firebase
        auth.signOut()

        // Cerrar sesión de Google
        googleSignInClient.signOut().addOnCompleteListener {
            // Volver a la pantalla de inicio de sesión
            val intent = Intent(this, A_Sesion::class.java)
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            finish()
        }
    }
}