package com.example.domus.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.ItemListaCompraBinding

class Adapt_ListaCompra(
    private val onItemClick: (Entity_ItemCompra) -> Unit,
    private val onItemLongClick: (Entity_ItemCompra) -> Unit
) : RecyclerView.Adapter<Adapt_ListaCompra.ViewHolder>() {

    private var items: List<Entity_ItemCompra> = emptyList()

    fun updateData(newItems: List<Entity_ItemCompra>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListaCompraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemListaCompraBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Entity_ItemCompra) {
            binding.tvNombreItemCompra.text = item.nombre
            binding.cbItemComprado.isChecked = item.comprado
            
            // Efecto visual de tachado si está comprado
            if (item.comprado) {
                binding.tvNombreItemCompra.paintFlags = binding.tvNombreItemCompra.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvNombreItemCompra.alpha = 0.5f
            } else {
                binding.tvNombreItemCompra.paintFlags = binding.tvNombreItemCompra.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvNombreItemCompra.alpha = 1.0f
            }

            // El checkbox es solo visual, el click lo gestiona la fila
            binding.cbItemComprado.isClickable = false
        }
    }
}
