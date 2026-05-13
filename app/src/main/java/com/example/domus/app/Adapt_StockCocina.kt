package com.example.domus.app

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.R
import com.example.domus.databinding.ItemAlmacenHeaderBinding
import com.example.domus.databinding.ItemStockCocinaBinding

sealed class StockDisplayItem {
    data class Header(val almacen: Almacen, val isExpanded: Boolean, val productCount: Int) : StockDisplayItem()
    data class Product(val producto: Producto) : StockDisplayItem()
}

class Adapt_StockCocina(
    private val onItemClick: (Producto) -> Unit,
    private val onPlusClick: (Producto) -> Unit,
    private val onMinusClick: (Producto) -> Unit,
    private val onHeaderClick: (Almacen) -> Unit,
    private val onAlmacenEdit: (Almacen) -> Unit,
    private val onAlmacenDelete: (Almacen) -> Unit
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
            TYPE_HEADER -> HeaderViewHolder(ItemAlmacenHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> ProductViewHolder(ItemStockCocinaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is StockDisplayItem.Header -> (holder as HeaderViewHolder).bind(item, onHeaderClick, onAlmacenEdit, onAlmacenDelete)
            is StockDisplayItem.Product -> (holder as ProductViewHolder).bind(item.producto, onItemClick, onPlusClick, onMinusClick)
        }
    }

    class HeaderViewHolder(private val binding: ItemAlmacenHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: StockDisplayItem.Header, onHeaderClick: (Almacen) -> Unit, onAlmacenEdit: (Almacen) -> Unit, onAlmacenDelete: (Almacen) -> Unit) {
            binding.tvAlmacenNombre.text = header.almacen.nombre
            binding.tvProductCount.text = "${header.productCount} productos"
            
            binding.ivExpandIcon.rotation = if (header.isExpanded) 0f else -90f
            
            binding.root.setOnClickListener { onHeaderClick(header.almacen) }
            
            // Ocultar opciones para la sección por defecto "Desconocido"
            if (header.almacen.id == "desconocido") {
                binding.btnMoreOptions.visibility = View.GONE
            } else {
                binding.btnMoreOptions.visibility = View.VISIBLE
                binding.btnMoreOptions.setOnClickListener { view ->
                    val popup = PopupMenu(view.context, view)
                    popup.menu.add("Cambiar nombre")
                    popup.menu.add("Eliminar almacén")
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title) {
                            "Cambiar nombre" -> onAlmacenEdit(header.almacen)
                            "Eliminar almacén" -> onAlmacenDelete(header.almacen)
                        }
                        true
                    }
                    popup.show()
                }
            }
        }
    }

    class ProductViewHolder(private val binding: ItemStockCocinaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto, onItemClick: (Producto) -> Unit, onPlusClick: (Producto) -> Unit, onMinusClick: (Producto) -> Unit) {
            binding.tvNombreProductoStock.text = producto.nombre
            binding.tvDescripcionProductoStock.text = producto.marca
            
            val displayCant = if (producto.cantidad % 1.0 == 0.0) producto.cantidad.toInt().toString() else String.format("%.2f", producto.cantidad)
            binding.tvCantidadProductoStock.text = "Cant: $displayCant"
            
            if (!producto.fotoBase64.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(producto.fotoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivProductoStock.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.ivProductoStock.setImageResource(com.google.android.material.R.drawable.navigation_empty_icon)
                }
            } else {
                binding.ivProductoStock.setImageResource(com.google.android.material.R.drawable.navigation_empty_icon)
            }

            if (producto.cantidad <= 0.25) {
                binding.btnRestarCantidad.setIconResource(android.R.drawable.ic_menu_delete)
            } else {
                binding.btnRestarCantidad.setIconResource(R.drawable.ic_minus)
            }

            binding.root.setOnClickListener { onItemClick(producto) }
            binding.btnSumarCantidad.setOnClickListener { onPlusClick(producto) }
            binding.btnRestarCantidad.setOnClickListener { onMinusClick(producto) }
        }
    }

    class StockDiffCallback : DiffUtil.ItemCallback<StockDisplayItem>() {
        override fun areItemsTheSame(oldItem: StockDisplayItem, newItem: StockDisplayItem): Boolean {
            return if (oldItem is StockDisplayItem.Header && newItem is StockDisplayItem.Header) {
                oldItem.almacen.id == newItem.almacen.id
            } else if (oldItem is StockDisplayItem.Product && newItem is StockDisplayItem.Product) {
                oldItem.producto.id == newItem.producto.id
            } else false
        }
        override fun areContentsTheSame(oldItem: StockDisplayItem, newItem: StockDisplayItem) = oldItem == newItem
    }
}
