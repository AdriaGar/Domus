package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.databinding.FragmentStockCocinaBinding

class F_StockCocina : Fragment() {

    private var _binding: FragmentStockCocinaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockCocinaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productos = listOf(
            Producto("Tomates", "Tomates pera para ensalada", 5),
            Producto("Leche", "Leche entera", 2),
            Producto("Pan de molde", "Pan de molde sin corteza", 1)
        )

        binding.tvStockCount.text = "${productos.size} Productos"

        binding.rvStockCocina.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = Adapt_StockCocina(productos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
