package com.example.domus.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.domus.R
import com.example.domus.app.viewModel.User
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.ItemTransaccionBinding
import de.hdodenhof.circleimageview.CircleImageView

class Adapt_Transaccion : ListAdapter<Transaccion, Adapt_Transaccion.TransaccionViewHolder>(TransaccionDiffCallback()) {

    private var users: List<User> = emptyList()

    fun setUsers(users: List<User>) {
        this.users = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaccionViewHolder {
        val binding = ItemTransaccionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransaccionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransaccionViewHolder, position: Int) {
        val transaccion = getItem(position)
        holder.itemView.setOnClickListener {
            val action = F_CuentasDirections.actionFCuentasToFAnadirGasto(transaccion.id, "Detalle de Transacción")
            it.findNavController().navigate(action)
        }
        holder.bind(transaccion, users)
    }

    class TransaccionViewHolder(private val binding: ItemTransaccionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private fun formatName(fullName: String): String {
            val parts = fullName.trim().split("\\s+".toRegex())
            return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else fullName
        }

        fun bind(transaccion: Transaccion, allUsers: List<User>) {
            val context = binding.root.context
            binding.tvDescripcion.text = transaccion.descripcion
            binding.tvUsuario.text = "Pagado por ${formatName(transaccion.usuarioNombre)}"

            // Buscar la foto del usuario pagador
            val payer = allUsers.find { it.uid == transaccion.usuarioId }
            Glide.with(context)
                .load(payer?.photoUrl)
                .placeholder(R.drawable.ic_user_default)
                .error(R.drawable.ic_user_default)
                .into(binding.ivUsuarioPagador)

            binding.llParticipantesFotos.removeAllViews()

            when (transaccion.tipo) {
                TipoTransaccion.GASTO.name -> {
                    binding.tvCantidad.text = String.format("-%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_gasto))
                    
                    if (transaccion.participantes.isNotEmpty()) {
                        binding.llParticipantesFotos.visibility = View.VISIBLE
                        
                        // Añadir fotos de los participantes amontonadas
                        val participantesAMostrar = transaccion.participantes.take(4)
                        participantesAMostrar.forEachIndexed { index, userId ->
                            val user = allUsers.find { it.uid == userId }
                            val imageView = CircleImageView(context).apply {
                                val size = (20 * context.resources.displayMetrics.density).toInt()
                                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                    // NO aplicamos margen negativo al último elemento para evitar que se corte a la derecha
                                    if (index < participantesAMostrar.size - 1) {
                                        marginEnd = (-6 * context.resources.displayMetrics.density).toInt()
                                    }
                                }
                                borderWidth = (1 * context.resources.displayMetrics.density).toInt()
                                borderColor = ContextCompat.getColor(context, android.R.color.white)
                            }
                            
                            Glide.with(context)
                                .load(user?.photoUrl)
                                .placeholder(R.drawable.ic_user_default)
                                .error(R.drawable.ic_user_default)
                                .into(imageView)
                            
                            binding.llParticipantesFotos.addView(imageView)
                        }
                    } else {
                        binding.llParticipantesFotos.visibility = View.GONE
                    }
                }
                TipoTransaccion.INGRESO.name -> {
                    binding.tvCantidad.text = String.format("+%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_ingreso))
                    binding.llParticipantesFotos.visibility = View.GONE
                }
                TipoTransaccion.TRANSFERENCIA.name -> {
                    binding.tvCantidad.text = String.format("%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_transferencia))
                    
                    if (transaccion.participantes.isNotEmpty()) {
                        binding.llParticipantesFotos.visibility = View.VISIBLE
                        val targetId = transaccion.participantes.first()
                        val targetUser = allUsers.find { it.uid == targetId }
                        
                        val imageView = CircleImageView(context).apply {
                            val size = (20 * context.resources.displayMetrics.density).toInt()
                            layoutParams = LinearLayout.LayoutParams(size, size)
                            borderWidth = (1 * context.resources.displayMetrics.density).toInt()
                            borderColor = ContextCompat.getColor(context, android.R.color.white)
                        }
                        
                        Glide.with(context)
                            .load(targetUser?.photoUrl)
                            .placeholder(R.drawable.ic_user_default)
                            .error(R.drawable.ic_user_default)
                            .into(imageView)
                        
                        binding.llParticipantesFotos.addView(imageView)
                    } else {
                        binding.llParticipantesFotos.visibility = View.GONE
                    }
                }
            }
        }
    }

    class TransaccionDiffCallback : DiffUtil.ItemCallback<Transaccion>() {
        override fun areItemsTheSame(oldItem: Transaccion, newItem: Transaccion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaccion, newItem: Transaccion): Boolean {
            return oldItem == newItem
        }
    }
}
