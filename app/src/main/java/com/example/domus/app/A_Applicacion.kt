package com.example.domus.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.domus.R
import com.example.domus.app.viewModel.VM_Familia
import com.example.domus.databinding.ActivityApplicacionBinding
import com.example.domus.sesion.A_Sesion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class A_Applicacion : AppCompatActivity() {

    private lateinit var binding: ActivityApplicacionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var navController: NavController
    
    private val familyViewModel: VM_Familia by viewModels()
    private var ivSync: ImageView? = null
    private var profileImageView: CircleImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setSupportActionBar(binding.topAppBar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.f_Cuentas, R.id.f_ListaCompra, R.id.f_StockCocina, R.id.f_Tareas)
        )
        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)

        binding.bottomNavView.setOnItemSelectedListener { item ->
            navController.navigate(item.itemId, null, navOptions {
                launchSingleTop = true
                restoreState = false
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
            })
            true
        }

        observeSyncState()
        observeUserProfile()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)

        val profileMenuItem = menu?.findItem(R.id.menu_user_profile)
        val actionView = profileMenuItem?.actionView
        
        ivSync = actionView?.findViewById(R.id.iv_sync_icon)
        profileImageView = actionView?.findViewById(R.id.iv_profile_image)

        ivSync?.let { iv ->
            iv.visibility = View.VISIBLE
            iv.setOnClickListener {
                Toast.makeText(this, "Sincronizando...", Toast.LENGTH_SHORT).show()
                familyViewModel.syncWithFirebase()
            }
        }

        profileImageView?.setOnClickListener {
            showProfileMenu(it)
        }

        return true
    }

    private fun observeUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        // Escuchamos los cambios del perfil del usuario actual en tiempo real
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val photoUrl = snapshot.getString("photoUrl")
                    val lastUpdated = snapshot.getLong("lastUpdated") ?: 0L
                    
                    profileImageView?.let { iv ->
                        Glide.with(this)
                            .load(photoUrl)
                            .signature(ObjectKey(lastUpdated))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_user_default)
                            .into(iv)
                    }
                }
            }
    }

    private fun observeSyncState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                familyViewModel.isSyncing.collect { syncing ->
                    toggleSyncAnimation(syncing)
                }
            }
        }
    }

    private fun toggleSyncAnimation(syncing: Boolean) {
        ivSync?.let { iv ->
            if (syncing) {
                val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                rotate.duration = 1000
                rotate.repeatCount = Animation.INFINITE
                rotate.interpolator = LinearInterpolator()
                iv.startAnimation(rotate)
                iv.alpha = 1.0f
            } else {
                iv.clearAnimation()
                iv.alpha = 0.6f
            }
        }
    }

    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_manage_family -> {
                    navController.navigate(R.id.f_GestionarFamilia)
                    true
                }
                R.id.action_sign_out -> {
                    showSignOutDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar la sesión?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ -> signOut() }
            .show()
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, A_Sesion::class.java)
            startActivity(intent)
            finish()
        }
    }
}
