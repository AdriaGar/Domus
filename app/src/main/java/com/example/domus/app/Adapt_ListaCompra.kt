package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.ItemListaCompraBinding


class Adapt_ListaCompra(private val items: List<Entity_ItemCompra>) :
    RecyclerView.Adapter<Adapt_ListaCompra.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListaCompraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemListaCompraBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Entity_ItemCompra) {
            binding.tvNombreItemCompra.text = item.nombre
            binding.cbItemComprado.isChecked = item.comprado
        }
    }
}