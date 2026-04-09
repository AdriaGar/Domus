package com.example.domus.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.domus.app.viewModel.StockCocinaViewModelFactory
import com.example.domus.app.viewModel.VM_StockCocina
import com.example.domus.databinding.FragmentAnadirProductoStockBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class F_AnadirProductoStock : Fragment() {

    private var _binding: FragmentAnadirProductoStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_StockCocina by viewModels {
        StockCocinaViewModelFactory(requireActivity().application)
    }

    private var listaAlmacenes: List<Almacen> = emptyList()
    private var almacenSeleccionadoId: String? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.tietBarcode.setText(result.contents)
            buscarProductoPorCodigo(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnadirProductoStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeAlmacenes()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun observeAlmacenes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allAlmacenes.collectLatest { almacenes ->
                listaAlmacenes = almacenes
                val nombres = almacenes.map { it.nombre }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
                binding.spinnerAlmacen.setAdapter(adapter)
                
                binding.spinnerAlmacen.setOnItemClickListener { _, _, position, _ ->
                    almacenSeleccionadoId = almacenes[position].id
                    binding.tilAlmacen.error = null
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnScanBarcode.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            options.setPrompt("Escanea el código del producto")
            options.setBeepEnabled(true)
            options.setOrientationLocked(false)
            barcodeLauncher.launch(options)
        }

        binding.btnSaveProduct.setOnClickListener {
            guardarProducto()
        }
    }

    private fun buscarProductoPorCodigo(barcode: String) {
        lifecycleScope.launch {
            val productoEncontrado = viewModel.findProductByBarcode(barcode)
            if (productoEncontrado != null) {
                binding.tietNombre.setText(productoEncontrado.nombre)
                binding.tietMarca.setText(productoEncontrado.marca)
                binding.tietCategoria.setText(productoEncontrado.categoria)
                binding.tietInfoNutricional.setText(productoEncontrado.infoNutricional)
                
                val almacen = listaAlmacenes.find { it.id == productoEncontrado.almacenId }
                almacen?.let {
                    binding.spinnerAlmacen.setText(it.nombre, false)
                    almacenSeleccionadoId = it.id
                }
                
                Toast.makeText(requireContext(), "Producto encontrado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Producto no registrado. Introduce los datos manualmente.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun guardarProducto() {
        val nombre = binding.tietNombre.text.toString().trim()
        val marca = binding.tietMarca.text.toString().trim()
        val categoria = binding.tietCategoria.text.toString().trim()
        val infoNutri = binding.tietInfoNutricional.text.toString().trim()
        val cant = binding.tietCantidad.text.toString().toIntOrNull() ?: 1
        val barcode = binding.tietBarcode.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.tilNombre.error = "El nombre es obligatorio"
            return
        }

        if (almacenSeleccionadoId == null) {
            binding.tilAlmacen.error = "Debes seleccionar un almacén"
            return
        }

        viewModel.addProducto(
            nombre = nombre,
            marca = marca,
            cantidad = cant,
            almacenId = almacenSeleccionadoId!!,
            categoria = categoria,
            infoNutricional = if (infoNutri.isEmpty()) null else infoNutri,
            barcode = if (barcode.isEmpty()) null else barcode
        )
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
