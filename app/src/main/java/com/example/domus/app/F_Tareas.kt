package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.databinding.FragmentTareasBinding

class F_Tareas : Fragment() {

    private lateinit var binding: FragmentTareasBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTareasBinding.inflate(inflater, container, false)

        val tareas = listOf(
            Tarea("Limpiar la cocina", "Fregar los platos y limpiar la encimera", false),
            Tarea("Hacer la compra", "Comprar leche, pan y huevos", true),
            Tarea("Lavar la ropa", "Separar la ropa de color y la blanca", false)
        )

        binding.rvTareas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTareas.adapter = Adapt_Tarea(tareas)

        return binding.root
    }
}