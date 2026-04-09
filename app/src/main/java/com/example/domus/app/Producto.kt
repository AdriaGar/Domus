package com.example.domus.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_cocina")
data class Producto(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val marca: String = "",
    val infoNutricional: String? = null,
    val fotoUrl: String? = null,
    val categoria: String = "",
    var cantidad: Int = 0,
    val barcode: String? = null,
    val almacenId: String = "", // Referencia al almacén donde está guardado
    val familiaId: String? = null,
    val usuarioId: String = ""
)
