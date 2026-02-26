package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.databinding.FragmentStockCocinaBinding

class F_StockCocina : Fragment() {

    private lateinit var binding: FragmentStockCocinaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStockCocinaBinding.inflate(inflater, container, false)

        val productos = listOf(
            Producto("Tomates", "Tomates pera para ensalada", 5),
            Producto("Leche", "Leche entera", 2),
            Producto("Pan de molde", "Pan de molde sin corteza", 1)
        )

        binding.rvStockCocina.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStockCocina.adapter = Adapt_StockCocina(productos)

        return binding.root
    }
}