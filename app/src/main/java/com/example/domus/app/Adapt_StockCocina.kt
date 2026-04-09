package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.R
import com.example.domus.databinding.ItemHistorialHeaderBinding
import com.example.domus.databinding.ItemStockCocinaBinding

sealed class StockDisplayItem {
    data class Header(val almacenNombre: String) : StockDisplayItem()
    data class Product(val producto: Producto) : StockDisplayItem()
}

class Adapt_StockCocina(
    private val onItemClick: (Producto) -> Unit,
    private val onDeleteClick: (Producto) -> Unit
) : ListAdapter<StockDisplayItem, RecyclerView.ViewHolder>(StockDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StockDisplayItem.Header -> TYPE_HEADER
            is StockDisplayItem.Product -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHistorialHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemStockCocinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ProductViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is StockDisplayItem.Header -> (holder as HeaderViewHolder).bind(item.almacenNombre)
            is StockDisplayItem.Product -> (holder as ProductViewHolder).bind(item.producto, onItemClick, onDeleteClick)
        }
    }

    class HeaderViewHolder(private val binding: ItemHistorialHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nombre: String) {
            binding.tvLoteFecha.text = nombre // Reutilizamos el layout de cabecera
        }
    }

    class ProductViewHolder(private val binding: ItemStockCocinaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto, onItemClick: (Producto) -> Unit, onDeleteClick: (Producto) -> Unit) {
            binding.tvNombreProductoStock.text = producto.nombre
            binding.tvDescripcionProductoStock.text = producto.marca
            binding.tvCantidadProductoStock.text = "Cant: ${producto.cantidad}"
            
            binding.root.setOnClickListener { onItemClick(producto) }
            binding.root.setOnLongClickListener {
                onDeleteClick(producto)
                true
            }
            binding.ivProductoStock.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    class StockDiffCallback : DiffUtil.ItemCallback<StockDisplayItem>() {
        override fun areItemsTheSame(oldItem: StockDisplayItem, newItem: StockDisplayItem): Boolean {
            return if (oldItem is StockDisplayItem.Header && newItem is StockDisplayItem.Header) {
                oldItem.almacenNombre == newItem.almacenNombre
            } else if (oldItem is StockDisplayItem.Product && newItem is StockDisplayItem.Product) {
                oldItem.producto.id == newItem.producto.id
            } else false
        }

        override fun areContentsTheSame(oldItem: StockDisplayItem, newItem: StockDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
