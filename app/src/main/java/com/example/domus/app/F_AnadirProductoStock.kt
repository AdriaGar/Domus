package com.example.domus.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.domus.app.viewModel.StockCocinaViewModelFactory
import com.example.domus.app.viewModel.VM_StockCocina
import com.example.domus.databinding.FragmentAnadirProductoStockBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class F_AnadirProductoStock : Fragment() {

    private var _binding: FragmentAnadirProductoStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_StockCocina by viewModels {
        StockCocinaViewModelFactory(requireActivity().application)
    }

    private var listaAlmacenes: List<Almacen> = emptyList()
    private var almacenSeleccionadoId: String? = null
    private var fotoBase64: String? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.tietBarcode.setText(result.contents)
            buscarProductoPorCodigo(result.contents)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            processImage(it)
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
        binding.cardFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

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

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    
                    // Redimensionar a max 200px
                    val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                    val width = 200
                    val height = (width / ratio).toInt()
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                    
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    Base64.encodeToString(byteArray, Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
            }
            fotoBase64 = base64
            Glide.with(this@F_AnadirProductoStock).load(uri).into(binding.ivProducto)
            binding.ivProducto.setPadding(0, 0, 0, 0)
        }
    }

    private fun buscarProductoPorCodigo(barcode: String) {
        lifecycleScope.launch {
            val p = viewModel.findProductByBarcode(barcode)
            if (p != null) {
                binding.tietNombre.setText(p.nombre)
                binding.tietMarca.setText(p.marca)
                binding.tietCategoria.setText(p.categoria)
                binding.tietEnergia.setText(p.energia)
                binding.tietGrasas.setText(p.grasas)
                binding.tietGrasasSat.setText(p.grasasSaturadas)
                binding.tietHidratos.setText(p.hidratos)
                binding.tietAzucares.setText(p.azucares)
                binding.tietProteinas.setText(p.proteinas)
                binding.tietSal.setText(p.sal)
                
                if (!p.fotoBase64.isNullOrEmpty()) {
                    fotoBase64 = p.fotoBase64
                    val imageBytes = Base64.decode(p.fotoBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivProducto.setImageBitmap(decodedImage)
                    binding.ivProducto.setPadding(0, 0, 0, 0)
                }
                
                Toast.makeText(requireContext(), "Producto encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarProducto() {
        val nombre = binding.tietNombre.text.toString().trim()
        val marca = binding.tietMarca.text.toString().trim()
        val categoria = binding.tietCategoria.text.toString().trim()
        val cant = binding.tietCantidad.text.toString().toIntOrNull() ?: 1
        val barcode = binding.tietBarcode.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.tilNombre.error = "Obligatorio"
            return
        }

        if (almacenSeleccionadoId == null) {
            binding.tilAlmacen.error = "Selecciona almacén"
            return
        }

        viewModel.addProductoCompleto(
            nombre = nombre,
            marca = marca,
            cantidad = cant,
            almacenId = almacenSeleccionadoId!!,
            categoria = categoria,
            barcode = if (barcode.isEmpty()) null else barcode,
            energia = binding.tietEnergia.text.toString(),
            grasas = binding.tietGrasas.text.toString(),
            grasasSaturadas = binding.tietGrasasSat.text.toString(),
            hidratos = binding.tietHidratos.text.toString(),
            azucares = binding.tietAzucares.text.toString(),
            proteinas = binding.tietProteinas.text.toString(),
            sal = binding.tietSal.text.toString(),
            fotoBase64 = fotoBase64
        )
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
