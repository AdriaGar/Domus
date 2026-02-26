package com.example.domus.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domus.data.Entity.Entity_ItemCompra
import com.example.domus.databinding.FragmentListaCompraBinding

class F_ListaCompra : Fragment() {

    private lateinit var binding: FragmentListaCompraBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListaCompraBinding.inflate(inflater, container, false)

        val items = listOf(
            Entity_ItemCompra("Leche", false),
            Entity_ItemCompra("Pan", true),
            Entity_ItemCompra("Huevos", false)
        )

        binding.rvListaCompra.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListaCompra.adapter = Adapt_ListaCompra(items)

        return binding.root
    }
}