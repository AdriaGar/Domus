package com.example.domus.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.domus.R
import com.example.domus.app.viewModel.User
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.ItemListaCompraBinding

class Adapt_ListaCompra(
    private val onItemClick: (Entity_ItemCompra) -> Unit,
    private val onItemLongClick: (Entity_ItemCompra) -> Unit
) : ListAdapter<Entity_ItemCompra, Adapt_ListaCompra.ViewHolder>(ItemCompraDiffCallback()) {

    private var users: List<User> = emptyList()

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        // No notificamos aquí para dejar que submitList maneje la actualización de datos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListaCompraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val user = users.find { it.uid == item.usuarioId }
        holder.bind(item, user, onItemClick, onItemLongClick)
    }

    class ViewHolder(private val binding: ItemListaCompraBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            item: Entity_ItemCompra, 
            user: User?,
            onItemClick: (Entity_ItemCompra) -> Unit,
            onItemLongClick: (Entity_ItemCompra) -> Unit
        ) {
            binding.tvNombreItemCompra.text = item.nombre
            binding.cbItemComprado.isChecked = item.comprado
            
            if (item.comprado) {
                binding.tvNombreItemCompra.paintFlags = binding.tvNombreItemCompra.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvNombreItemCompra.alpha = 0.5f
            } else {
                binding.tvNombreItemCompra.paintFlags = binding.tvNombreItemCompra.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvNombreItemCompra.alpha = 1.0f
            }

            binding.cbItemComprado.isClickable = false
            
            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }

            // Cargar la foto del usuario
            Glide.with(binding.ivAddedBy.context)
                .load(user?.photoUrl)
                .signature(ObjectKey(user?.lastUpdated ?: 0L))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_user_default)
                .error(R.drawable.ic_user_default)
                .into(binding.ivAddedBy)
        }
    }

    class ItemCompraDiffCallback : DiffUtil.ItemCallback<Entity_ItemCompra>() {
        override fun areItemsTheSame(oldItem: Entity_ItemCompra, newItem: Entity_ItemCompra): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Entity_ItemCompra, newItem: Entity_ItemCompra): Boolean {
            return oldItem == newItem
        }
    }
}
