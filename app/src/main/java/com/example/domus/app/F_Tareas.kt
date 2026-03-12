package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.databinding.FragmentTareasBinding

data class Tarea(val nombre: String, val descripcion: String, val completada: Boolean)

class F_Tareas : Fragment() {

    private var _binding: FragmentTareasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTareasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tareas = listOf(
            Tarea("Limpiar la cocina", "Fregar los platos y limpiar la encimera", false),
            Tarea("Hacer la compra", "Comprar leche, pan y huevos", true),
            Tarea("Lavar la ropa", "Separar la ropa de color y la blanca", false)
        )

        // Actualizamos el contador siguiendo la nueva estética
        binding.tvTareasCount.text = "${tareas.count { !it.completada }} Pendientes"

        binding.rvTareas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = Adapt_Tarea(tareas)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}