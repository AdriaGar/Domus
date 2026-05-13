package com.example.domus.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.domus.app.viewModel.StockCocinaViewModelFactory
import com.example.domus.app.viewModel.VM_StockCocina
import com.example.domus.databinding.FragmentAnadirProductoStockBinding
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class F_AnadirProductoStock : Fragment() {

    private var _binding: FragmentAnadirProductoStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VM_StockCocina by viewModels {
        StockCocinaViewModelFactory(requireActivity().application)
    }

    private var listaAlmacenes: List<Almacen> = emptyList()
    private var almacenSeleccionadoId: String? = null
    private var fotoBase64: String? = null
    private var latestTmpUri: Uri? = null
    
    private var productoId: String? = null
    private var productoExistente: Producto? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            latestTmpUri?.let { uri ->
                processImage(uri)
            }
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
        
        productoId = arguments?.getString("productoId")
        
        setupToolbar()
        setupListeners()
        observeAlmacenes()
        
        if (productoId != null) {
            cargarProducto(productoId!!)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = if (productoId == null) "Añadir Producto" else "Editar Producto"
    }

    private fun cargarProducto(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val productos = viewModel.allProductos.first()
            productoExistente = productos.find { it.id == id }
            
            productoExistente?.let { p ->
                binding.tietNombre.setText(p.nombre)
                binding.tietMarca.setText(p.marca)
                binding.tietCategoria.setText(p.categoria)
                binding.tietBarcode.setText(p.barcode)
                binding.tietCantidad.setText(p.cantidad.toString())
                
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

                almacenSeleccionadoId = p.almacenId
                actualizarSpinnerAlmacen()

                // Bloquear campos inicialmente
                setFieldsEnabled(false)
                binding.btnSugerirCambio.visibility = View.VISIBLE
            }
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        binding.tilNombre.isEnabled = enabled
        binding.tilMarca.isEnabled = enabled
        binding.tilCategoria.isEnabled = enabled
        binding.tilBarcode.isEnabled = enabled
        binding.cardFoto.isEnabled = enabled
        binding.btnScanBarcode.isEnabled = enabled
        
        // GridLayout de info nutricional
        for (i in 0 until binding.glNutricional.childCount) {
            binding.glNutricional.getChildAt(i).isEnabled = enabled
        }
    }

    private fun actualizarSpinnerAlmacen() {
        val almacen = listaAlmacenes.find { it.id == almacenSeleccionadoId }
        almacen?.let {
            binding.spinnerAlmacen.setText(it.nombre, false)
        }
    }

    private fun observeAlmacenes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allAlmacenes.collect { almacenes ->
                listaAlmacenes = almacenes
                val nombres = almacenes.map { it.nombre }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
                binding.spinnerAlmacen.setAdapter(adapter)
                
                binding.spinnerAlmacen.setOnItemClickListener { _, _, position, _ ->
                    almacenSeleccionadoId = almacenes[position].id
                    binding.tilAlmacen.error = null
                }
                
                if (productoId != null) {
                    actualizarSpinnerAlmacen()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.cardFoto.setOnClickListener {
            openCamera()
        }

        binding.btnScanBarcode.setOnClickListener {
            startGoogleScanner()
        }

        binding.btnSugerirCambio.setOnClickListener {
            setFieldsEnabled(true)
            binding.btnSugerirCambio.visibility = View.GONE
            Toast.makeText(requireContext(), "Ahora puedes editar los datos", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveProduct.setOnClickListener {
            guardarProducto()
        }
    }

    private fun startGoogleScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null) {
                    binding.tietBarcode.setText(rawValue)
                    buscarProductoPorCodigo(rawValue)
                }
            }
    }

    private fun openCamera() {
        lifecycleScope.launch {
            try {
                val uri = getTmpFileUri()
                latestTmpUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", requireContext().externalCacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", tmpFile)
    }

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    
                    val exif = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    val rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

                    val ratio = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
                    val width = 300
                    val height = (width / ratio).toInt()
                    val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)
                    
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
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
            }
        }
    }

    private fun guardarProducto() {
        val nombre = binding.tietNombre.text.toString().trim()
        val marca = binding.tietMarca.text.toString().trim()
        val categoria = binding.tietCategoria.text.toString().trim()
        val cant = binding.tietCantidad.text.toString().toDoubleOrNull() ?: 1.0
        val barcode = binding.tietBarcode.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.tilNombre.error = "Obligatorio"
            return
        }

        if (almacenSeleccionadoId == null) {
            binding.tilAlmacen.error = "Selecciona almacén"
            return
        }

        val productoData = (productoExistente ?: Producto()).copy(
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

        if (productoId == null) {
            viewModel.addProductoCompleto(
                nombre = productoData.nombre,
                marca = productoData.marca,
                cantidad = productoData.cantidad,
                almacenId = productoData.almacenId,
                categoria = productoData.categoria,
                barcode = productoData.barcode,
                energia = productoData.energia,
                grasas = productoData.grasas,
                grasasSaturadas = productoData.grasasSaturadas,
                hidratos = productoData.hidratos,
                azucares = productoData.azucares,
                proteinas = productoData.proteinas,
                sal = productoData.sal,
                fotoBase64 = productoData.fotoBase64
            )
        } else {
            viewModel.updateProducto(productoData)
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
