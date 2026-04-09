package com.example.domus.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_cocina")
data class Producto(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val marca: String = "",
    val categoria: String = "",
    val fotoUrl: String? = null,
    val fotoBase64: String? = null, // Imagen comprimida en Base64
    val barcode: String? = null,
    
    // Información Nutricional (EU Standard)
    val energia: String? = null,
    val grasas: String? = null,
    val grasasSaturadas: String? = null,
    val hidratos: String? = null,
    val azucares: String? = null,
    val proteinas: String? = null,
    val sal: String? = null,

    // Datos de Stock
    var cantidad: Int = 0,
    val almacenId: String = "",
    val familiaId: String? = null,
    val usuarioId: String = ""
)
