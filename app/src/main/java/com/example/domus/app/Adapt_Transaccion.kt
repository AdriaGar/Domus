package com.example.domus.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.R
import com.example.domus.app.F_CuentasDirections
import com.example.domus.data.Entity.TipoTransaccion
import com.example.domus.data.Entity.Transaccion
import com.example.domus.databinding.ItemTransaccionBinding

class Adapt_Transaccion : ListAdapter<Transaccion, Adapt_Transaccion.TransaccionViewHolder>(TransaccionDiffCallback()) {

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
        holder.bind(transaccion)
    }

    class TransaccionViewHolder(private val binding: ItemTransaccionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaccion: Transaccion) {
            binding.tvDescripcion.text = transaccion.descripcion
            binding.tvUsuario.text = "Pagado por ${transaccion.usuarioNombre}"

            val context = binding.root.context
            when (transaccion.tipo) {
                TipoTransaccion.GASTO.name -> {
                    binding.tvCantidad.text = String.format("-%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_gasto))
                    
                    if (transaccion.participantes.isNotEmpty()) {
                        binding.tvParticipantes.visibility = View.VISIBLE
                        binding.tvParticipantes.text = "(entre ${transaccion.participantes.size} personas)"
                    } else {
                        binding.tvParticipantes.visibility = View.GONE
                    }
                }
                TipoTransaccion.INGRESO.name -> {
                    binding.tvCantidad.text = String.format("+%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_ingreso))
                    binding.tvParticipantes.visibility = View.GONE
                }
                TipoTransaccion.TRANSFERENCIA.name -> {
                    binding.tvCantidad.text = String.format("%.2f €", transaccion.cantidad)
                    binding.tvCantidad.setTextColor(ContextCompat.getColor(context, R.color.importe_transferencia))
                    binding.tvParticipantes.visibility = View.GONE
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
