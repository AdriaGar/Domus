package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.domus.databinding.ItemTareaBinding

class Adapt_Tarea(private val tareas: List<Tarea>) : RecyclerView.Adapter<Adapt_Tarea.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTareaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    class ViewHolder(private val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: Tarea) {
            binding.tvTituloTarea.text = tarea.nombre
            binding.tvDescripcionTarea.text = tarea.descripcion
            binding.cbTareaCompletada.isChecked = tarea.completada
        }
    }
}
