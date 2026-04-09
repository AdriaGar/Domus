package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.app.viewModel.User
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.ItemHistorialHeaderBinding
import com.example.domus.databinding.ItemListaCompraBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class HistorialItem {
    data class Header(val fecha: Long) : HistorialItem()
    data class Product(val item: Entity_ItemCompra, val user: User?) : HistorialItem()
}

class Adapt_HistorialCompra(
    private val onItemClick: (Entity_ItemCompra) -> Unit,
    private val onItemLongClick: (Entity_ItemCompra) -> Unit
) : ListAdapter<HistorialItem, RecyclerView.ViewHolder>(HistorialDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistorialItem.Header -> TYPE_HEADER
            is HistorialItem.Product -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHistorialHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemListaCompraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ProductViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistorialItem.Header -> (holder as HeaderViewHolder).bind(item.fecha)
            is HistorialItem.Product -> (holder as ProductViewHolder).bind(item.item, item.user, onItemClick, onItemLongClick)
        }
    }

    class HeaderViewHolder(private val binding: ItemHistorialHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fecha: Long) {
            val sdf = SimpleDateFormat("'Compra del' dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
            binding.tvLoteFecha.text = sdf.format(Date(fecha))
        }
    }

    class ProductViewHolder(private val binding: ItemListaCompraBinding) : RecyclerView.ViewHolder(binding.root) {
        private val baseViewHolder = Adapt_ListaCompra.ViewHolder(binding)
        fun bind(item: Entity_ItemCompra, user: User?, onClick: (Entity_ItemCompra) -> Unit, onLongClick: (Entity_ItemCompra) -> Unit) {
            baseViewHolder.bind(item, user, onClick, onLongClick)
        }
    }

    class HistorialDiffCallback : DiffUtil.ItemCallback<HistorialItem>() {
        override fun areItemsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean {
            return if (oldItem is HistorialItem.Header && newItem is HistorialItem.Header) {
                oldItem.fecha == newItem.fecha
            } else if (oldItem is HistorialItem.Product && newItem is HistorialItem.Product) {
                oldItem.item.id == newItem.item.id
            } else false
        }

        override fun areContentsTheSame(oldItem: HistorialItem, newItem: HistorialItem): Boolean {
            return oldItem == newItem
        }
    }
}
