package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.R
import com.example.domus.databinding.ItemStockCocinaBinding

class Adapt_StockCocina(private val productos: List<Producto>) :
    RecyclerView.Adapter<Adapt_StockCocina.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockCocinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    class ViewHolder(private val binding: ItemStockCocinaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto) {
            binding.tvNombreProductoStock.text = producto.nombre
            binding.tvDescripcionProductoStock.text = producto.descripcion
            binding.tvCantidadProductoStock.text = "Cantidad: ${producto.cantidad}"
            binding.ivProductoStock.setImageResource(R.drawable.ic_launcher_background)
        }
    }
}
