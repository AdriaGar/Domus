package com.example.domus.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "almacenes")
data class Almacen(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val icono: String? = null, // Podría ser un emoji o nombre de recurso
    val familiaId: String? = null,
    val usuarioId: String = ""
)
